package io.iohk.connector

import java.time.Instant
import java.util.concurrent.{Executor, TimeUnit}

import doobie.implicits._
import io.grpc._
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.iohk.atala.crypto.{EC, ECKeyPair, ECPublicKey, ECSignature}
import io.iohk.connector.model._
import io.iohk.connector.payments.BraintreePayments
import io.iohk.connector.repositories._
import io.iohk.connector.repositories.daos.{ConnectionTokensDAO, ConnectionsDAO, MessagesDAO, ParticipantsDAO}
import io.iohk.connector.services.{ConnectionsService, MessagesService, RegistrationService}
import io.iohk.cvp.ParticipantPropagatorService
import io.iohk.cvp.grpc.{
  GrpcAuthenticationHeader,
  GrpcAuthenticationHeaderParser,
  GrpcAuthenticatorInterceptor,
  SignedRequestsHelper
}
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.prism.protos.connector_api
import org.mockito.MockitoSugar._
import org.scalatest.BeforeAndAfterEach
import scalapb.GeneratedMessage

import scala.concurrent.duration.DurationLong

trait ApiTestHelper[STUB] {
  def apply[T](participantId: ParticipantId)(f: STUB => T): T
  def apply[T](requestNonce: Vector[Byte], signature: ECSignature, publicKey: ECPublicKey)(f: STUB => T): T
  def apply[T](requestNonce: Vector[Byte], keys: ECKeyPair, request: GeneratedMessage)(f: STUB => T): T = {
    val payload = SignedRequestsHelper.merge(RequestNonce(requestNonce), request.toByteArray).toArray
    val signature = EC.sign(payload.array, keys.privateKey)
    apply(requestNonce, signature, keys.publicKey)(f)
  }
  def unlogged[T](f: STUB => T): T
}

abstract class RpcSpecBase extends PostgresRepositorySpec with BeforeAndAfterEach {

  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _

  def services: Seq[ServerServiceDefinition]

  override def beforeEach(): Unit = {
    super.beforeEach()

    serverName = InProcessServerBuilder.generateName()

    val serverBuilderWithoutServices = InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .intercept(new GrpcAuthenticatorInterceptor)

    val serverBuilder = services.foldLeft(serverBuilderWithoutServices) { (builder, service) =>
      builder.addService(service)
    }

    serverHandle = serverBuilder.build().start()

    channelHandle = InProcessChannelBuilder.forName(serverName).directExecutor().build()
  }

  override def afterEach(): Unit = {
    channelHandle.shutdown()
    channelHandle.awaitTermination(10, TimeUnit.SECONDS)
    serverHandle.shutdown()
    serverHandle.awaitTermination()
    super.afterEach()
  }

  def usingApiAsConstructor[STUB](stubFactory: (ManagedChannel, CallOptions) => STUB): ApiTestHelper[STUB] =
    new ApiTestHelper[STUB] {
      override def unlogged[T](f: STUB => T): T = {
        val blockingStub = stubFactory(channelHandle, CallOptions.DEFAULT)
        f(blockingStub)
      }

      override def apply[T](id: ParticipantId)(f: STUB => T): T = {
        val callOptions = CallOptions.DEFAULT.withCallCredentials(new CallCredentials {
          override def applyRequestMetadata(
              requestInfo: CallCredentials.RequestInfo,
              appExecutor: Executor,
              applier: CallCredentials.MetadataApplier
          ): Unit = {
            appExecutor.execute { () =>
              applier.apply(GrpcAuthenticationHeader.Legacy(id).toMetadata)
            }
          }

          override def thisUsesUnstableApi(): Unit = ()
        })

        val blockingStub = stubFactory(channelHandle, callOptions)
        f(blockingStub)
      }
      override def apply[T](requestNonce: Vector[Byte], signature: ECSignature, publicKey: ECPublicKey)(
          f: STUB => T
      ): T = {

        val callOptions = CallOptions.DEFAULT.withCallCredentials(new CallCredentials {
          override def applyRequestMetadata(
              requestInfo: CallCredentials.RequestInfo,
              appExecutor: Executor,
              applier: CallCredentials.MetadataApplier
          ): Unit = {
            appExecutor.execute { () =>
              applier.apply(
                GrpcAuthenticationHeader
                  .PublicKeyBased(RequestNonce(requestNonce), publicKey, signature)
                  .toMetadata
              )
            }
          }

          override def thisUsesUnstableApi(): Unit = ()
        })

        val blockingStub = stubFactory(channelHandle, callOptions)
        f(blockingStub)
      }
    }
}

class ConnectorRpcSpecBase extends RpcSpecBase {
  implicit val executionContext = scala.concurrent.ExecutionContext.global

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)

  override def services =
    Seq(
      connector_api.ConnectorServiceGrpc
        .bindService(
          connectorService,
          executionContext
        )
    )

  val usingApiAs: ApiTestHelper[connector_api.ConnectorServiceGrpc.ConnectorServiceBlockingStub] =
    usingApiAsConstructor(
      new connector_api.ConnectorServiceGrpc.ConnectorServiceBlockingStub(_, _)
    )

  lazy val braintreePayments = BraintreePayments(BraintreePayments.Config(false, "none", "none", "none", "none"))
  lazy val connectionsRepository = new ConnectionsRepository.PostgresImpl(database)(executionContext)
  lazy val paymentsRepository = new PaymentsRepository(database)(executionContext)
  lazy val connectionsService =
    new ConnectionsService(connectionsRepository, paymentsRepository, braintreePayments, nodeMock)
  lazy val messagesRepository = new MessagesRepository(database)(executionContext)
  lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  lazy val participantsRepository = new ParticipantsRepository(database)(executionContext)

  lazy val nodeMock = mock[io.iohk.prism.protos.node_api.NodeServiceGrpc.NodeService]
  lazy val authenticator =
    new SignedRequestsAuthenticator(
      participantsRepository,
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )
  lazy val participantPropagator = new ParticipantPropagatorService(database)(executionContext)

  lazy val messagesService = new MessagesService(messagesRepository)
  lazy val registrationService = new RegistrationService(participantsRepository, nodeMock)(executionContext)
  lazy val connectorService = new ConnectorService(
    connectionsService,
    messagesService,
    registrationService,
    braintreePayments,
    paymentsRepository,
    authenticator,
    participantPropagator,
    nodeMock,
    participantsRepository,
    requireSignatureOnConnectionCreation = true
  )(
    executionContext
  )

  protected def createParticipant(
      name: String,
      tpe: ParticipantType,
      logo: Option[ParticipantLogo] = None,
      publicKey: Option[ECPublicKey] = None,
      did: Option[String] = None
  ): ParticipantId = {
    val id = ParticipantId.random()
    ParticipantsDAO
      .insert(ParticipantInfo(id, tpe, publicKey, name, did, logo))
      .transact(database)
      .unsafeToFuture()
      .futureValue

    id
  }

  protected def createHolder(name: String, publicKey: Option[ECPublicKey] = None): ParticipantId = {
    createParticipant(name, ParticipantType.Holder, publicKey = publicKey)
  }

  protected def createIssuer(
      name: String,
      publicKey: Option[ECPublicKey] = None,
      did: Option[String] = None
  ): ParticipantId = {
    createParticipant(name, ParticipantType.Issuer, Some(ParticipantLogo(Vector(10.toByte, 5.toByte))), publicKey, did)
  }

  protected def createVerifier(
      name: String,
      publicKey: Option[ECPublicKey] = None,
      did: Option[String] = None
  ): ParticipantId = {
    createParticipant(name, ParticipantType.Verifier, Some(ParticipantLogo(Vector(1.toByte, 3.toByte))), publicKey, did)
  }

  protected def createToken(initiator: ParticipantId): TokenString = {
    val tokenString = TokenString.random()
    ConnectionTokensDAO
      .insert(initiator, tokenString)
      .transact(database)
      .unsafeToFuture()
      .futureValue

    tokenString
  }

  protected def createConnection(
      initiatorId: ParticipantId,
      acceptorId: ParticipantId
  ): ConnectionId = {
    val token = createToken(initiatorId)
    ConnectionsDAO.insert(initiatorId, acceptorId, token).transact(database).unsafeToFuture().futureValue._1
  }

  protected def createConnection(
      initiatorId: ParticipantId,
      acceptorId: ParticipantId,
      instantiatedAt: Instant
  ): ConnectionId = {
    val token = createToken(initiatorId)
    ConnectionsDAO
      .insert(initiatorId, acceptorId, instantiatedAt, token)
      .transact(database)
      .unsafeToFuture()
      .futureValue
  }

  // creates connections (initiator -> acceptor):
  // Verifier -> Holder0 at zeroTime
  // Issuer0 -> Verifier at zeroTime + 1
  // Verifier -> Holder1 at zeroTime + 2
  // ...
  // Issuer12 -> Verifier at zeroTime + 25
  protected def createExampleConnections(verifierId: ParticipantId, zeroTime: Long): Seq[(String, ConnectionId)] = {
    (for (i <- (0 to 12)) yield {
      val holderName = s"Holder$i"
      val issuerName = s"Issuer$i"
      val holderId = createHolder(holderName)
      val issuerId = createIssuer(issuerName)

      val holderConnectionId = createConnection(verifierId, holderId, Instant.ofEpochMilli(zeroTime + 2 * i))
      val issuerConnectionId = createConnection(issuerId, verifierId, Instant.ofEpochMilli(zeroTime + 2 * i + 1))

      List(holderName -> holderConnectionId, issuerName -> issuerConnectionId)
    }).flatten
  }

  def createMessage(sender: ParticipantId, connectionId: ConnectionId, content: Array[Byte]): MessageId = {
    val messageId = MessageId.random()
    val query = for {
      recipientOption <- ConnectionsDAO.getOtherSide(connectionId, sender)
      recipient = recipientOption.getOrElse(
        throw new RuntimeException(
          s"Failed to send message, the connection $connectionId with sender $sender doesn't exist"
        )
      )
      _ <- MessagesDAO.insert(messageId, connectionId, sender, recipient, content)
    } yield messageId

    query.transact(database).unsafeToFuture().futureValue
  }

  protected def createExampleMessages(recipientId: ParticipantId): Seq[(MessageId, ConnectionId)] = {
    val acceptedConnections = for (i <- 1 to 6) yield {
      val issuer = createIssuer(s"Issuer$i")
      (createConnection(issuer, recipientId), issuer)
    }

    val initiatedConnections = for (i <- 1 to 2) yield {
      val holder = createHolder(s"Holder$i")
      (createConnection(recipientId, holder), holder)
    }

    val connections = (acceptedConnections ++ initiatedConnections).toIndexedSeq

    for (i <- 1 to 30) yield {
      val (connection, sender) = connections((i - 1) % connections.size)
      (createMessage(sender, connection, s"message$i".getBytes), connection)
    }
  }

}
