package io.iohk.connector

import java.time.Instant
import java.util.concurrent.{Executor, TimeUnit}

import doobie.implicits._
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc._
import io.iohk.connector.model._
import io.iohk.connector.payments.PaymentWall
import io.iohk.connector.protos.ConnectorServiceGrpc
import io.iohk.connector.repositories.daos.{ConnectionTokensDAO, ConnectionsDAO, MessagesDAO, ParticipantsDAO}
import io.iohk.connector.repositories.{ConnectionsRepository, MessagesRepository}
import io.iohk.connector.services.{ConnectionsService, MessagesService}
import io.iohk.cvp.repositories.PostgresRepositorySpec
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.duration.DurationLong

class RpcSpecBase extends PostgresRepositorySpec with BeforeAndAfterEach {
  override val tables = List("messages", "connections", "connection_tokens", "holder_public_keys", "participants")
  implicit val executionContext = scala.concurrent.ExecutionContext.global

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)

  lazy val connectionsRepository = new ConnectionsRepository(database)(executionContext)
  lazy val connectionsService = new ConnectionsService(connectionsRepository)
  lazy val messagesRepository = new MessagesRepository(database)(executionContext)
  lazy val messagesService = new MessagesService(messagesRepository)
  lazy val paymentWall = new PaymentWall
  lazy val connectorService = new ConnectorService(connectionsService, messagesService, paymentWall)(executionContext)

  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    serverName = InProcessServerBuilder.generateName()

    serverHandle = InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .intercept(new UserIdInterceptor)
      .addService(
        ConnectorServiceGrpc
          .bindService(new ConnectorService(connectionsService, messagesService, paymentWall), executionContext)
      )
      .build()
      .start()

    channelHandle = InProcessChannelBuilder.forName(serverName).directExecutor().build()
  }

  override def afterEach(): Unit = {
    channelHandle.shutdown()
    channelHandle.awaitTermination(10, TimeUnit.SECONDS)
    serverHandle.shutdown()
    serverHandle.awaitTermination()
    super.afterEach()
  }

  def usingApiAs[T](id: ParticipantId)(f: ConnectorServiceGrpc.ConnectorServiceBlockingStub => T): T = {
    val callOptions = CallOptions.DEFAULT.withCallCredentials(new CallCredentials {
      override def applyRequestMetadata(
          requestInfo: CallCredentials.RequestInfo,
          appExecutor: Executor,
          applier: CallCredentials.MetadataApplier
      ): Unit = {
        appExecutor.execute { () =>
          val headers = new Metadata()
          headers.put(UserIdInterceptor.USER_ID_METADATA_KEY, id.id.toString)
          applier.apply(headers)
        }
      }

      override def thisUsesUnstableApi(): Unit = ()
    })

    val blockingStub = new ConnectorServiceGrpc.ConnectorServiceBlockingStub(channelHandle, callOptions)
    f(blockingStub)
  }

  protected def createParticipant(name: String, tpe: ParticipantType): ParticipantId = {
    val id = ParticipantId.random()
    ParticipantsDAO.insert(ParticipantInfo(id, tpe, name, None)).transact(database).unsafeToFuture().futureValue

    id
  }

  protected def createHolder(name: String): ParticipantId = createParticipant(name, ParticipantType.Holder)
  protected def createIssuer(name: String): ParticipantId = createParticipant(name, ParticipantType.Issuer)
  protected def createVerifier(name: String): ParticipantId = createParticipant(name, ParticipantType.Verifier)

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
      recipient <- ConnectionsDAO.getOtherSide(connectionId, sender)
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
