package io.iohk.atala.prism.connector

import cats.effect.unsafe.implicits.global
import doobie.implicits._
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories._
import io.iohk.atala.prism.connector.repositories.daos.{
  ConnectionTokensDAO,
  ConnectionsDAO,
  MessagesDAO,
  ParticipantsDAO
}
import io.iohk.atala.prism.connector.services.{
  ConnectionsService,
  MessageNotificationService,
  MessagesService,
  RegistrationService
}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.connector_api
import io.iohk.atala.prism.{ApiTestHelper, DIDUtil, RpcSpecBase}
import io.iohk.atala.prism.utils.IOUtils._
import org.mockito.MockitoSugar._

import java.time.Instant
import java.util.UUID

class ConnectorRpcSpecBase extends RpcSpecBase with DIDUtil {
  override def services =
    Seq(
      connector_api.ConnectorServiceGrpc
        .bindService(
          connectorService,
          executionContext
        )
    )

  val usingApiAs: ApiTestHelper[
    connector_api.ConnectorServiceGrpc.ConnectorServiceBlockingStub
  ] =
    usingApiAsConstructor(
      new connector_api.ConnectorServiceGrpc.ConnectorServiceBlockingStub(_, _)
    )

  val usingAsyncApiAs: ApiTestHelper[connector_api.ConnectorServiceGrpc.ConnectorServiceStub] =
    usingApiAsConstructor(
      new connector_api.ConnectorServiceGrpc.ConnectorServiceStub(_, _)
    )

  lazy val connectionsRepository =
    ConnectionsRepository.unsafe(dbLiftedToTraceIdIO, testLogs)
  lazy val connectionsService =
    ConnectionsService.unsafe(connectionsRepository, nodeMock, testLogs)
  lazy val messagesRepository =
    MessagesRepository.unsafe(dbLiftedToTraceIdIO, testLogs)
  lazy val requestNoncesRepository =
    RequestNoncesRepository.unsafe(dbLiftedToTraceIdIO, testLogs)
  lazy val participantsRepository =
    ParticipantsRepository.unsafe(dbLiftedToTraceIdIO, testLogs)

  lazy val nodeMock =
    mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  lazy val authenticator =
    new ConnectorAuthenticator(
      participantsRepository,
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )

  lazy val messagesService =
    MessagesService.unsafe(messagesRepository, testLogs)
  lazy val registrationService =
    RegistrationService.unsafe(participantsRepository, nodeMock, testLogs)
  lazy val messageNotificationService = MessageNotificationService(database)
  lazy val connectorService = new ConnectorService(
    connectionsService,
    messagesService,
    registrationService,
    messageNotificationService,
    authenticator,
    nodeMock,
    participantsRepository
  )(
    executionContext,
    global
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    messageNotificationService.start()
  }

  override def afterAll(): Unit = {
    messageNotificationService.stop()
    super.afterAll()
  }

  protected def createParticipant(
      name: String,
      tpe: ParticipantType,
      logo: Option[ParticipantLogo] = None,
      publicKey: Option[ECPublicKey] = None,
      did: Option[DID] = None,
      operationId: Option[AtalaOperationId] = None
  ): ParticipantId = {
    val id = ParticipantId.random()
    ParticipantsDAO
      .insert(ParticipantInfo(id, tpe, publicKey, name, did, logo, operationId))
      .transact(database)
      .unsafeToFuture()
      .futureValue

    id
  }

  protected def createHolder(
      name: String,
      publicKey: Option[ECPublicKey] = None,
      did: Option[DID] = None
  ): ParticipantId = {
    createParticipant(
      name,
      ParticipantType.Holder,
      publicKey = publicKey,
      did = did
    )
  }

  protected def createIssuer(
      name: String,
      publicKey: Option[ECPublicKey] = None,
      did: Option[DID] = None
  ): ParticipantId = {
    createParticipant(
      name,
      ParticipantType.Issuer,
      Some(ParticipantLogo(Vector(10.toByte, 5.toByte))),
      publicKey,
      did
    )
  }

  protected def createVerifier(
      name: String,
      publicKey: Option[ECPublicKey] = None,
      did: Option[DID] = None
  ): ParticipantId = {
    createParticipant(
      name,
      ParticipantType.Verifier,
      Some(ParticipantLogo(Vector(1.toByte, 3.toByte))),
      publicKey,
      did
    )
  }

  protected def createToken(initiator: ParticipantId): TokenString = {
    val tokenString = TokenString.random()
    ConnectionTokensDAO
      .insert(initiator, List(tokenString))
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
    ConnectionsDAO
      .insert(
        initiatorId,
        acceptorId,
        token,
        ConnectionStatus.InvitationMissing
      )
      .transact(database)
      .unsafeToFuture()
      .futureValue
      ._1
  }

  protected def createConnection(
      initiatorId: ParticipantId,
      acceptorId: ParticipantId,
      token: TokenString
  ): ConnectionId = {
    ConnectionsDAO
      .insert(
        initiatorId,
        acceptorId,
        token,
        ConnectionStatus.InvitationMissing
      )
      .transact(database)
      .unsafeToFuture()
      .futureValue
      ._1
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
  protected def createExampleConnections(
      verifierId: ParticipantId,
      zeroTime: Long
  ): Seq[(String, ConnectionId)] = {
    (for (i <- (0 to 12)) yield {
      val holderName = s"Holder$i"
      val issuerName = s"Issuer$i"
      val holderId = createHolder(holderName)
      val issuerId = createIssuer(issuerName)

      val holderConnectionId = createConnection(
        verifierId,
        holderId,
        Instant.ofEpochMilli(zeroTime + 2 * i)
      )
      val issuerConnectionId = createConnection(
        issuerId,
        verifierId,
        Instant.ofEpochMilli(zeroTime + 2 * i + 1)
      )

      List(holderName -> holderConnectionId, issuerName -> issuerConnectionId)
    }).flatten
  }

  private def createMessage(
      sender: ParticipantId,
      connectionId: ConnectionId,
      content: Array[Byte]
  ): MessageId = {
    val messageId = MessageId.random()
    val query = for {
      recipientOption <- ConnectionsDAO.getOtherSide(connectionId, sender)
      recipient = recipientOption.getOrElse(
        throw new RuntimeException(
          s"Failed to send message, the connection $connectionId with sender $sender doesn't exist"
        )
      )
      _ <- MessagesDAO.insert(
        messageId,
        connectionId,
        sender,
        recipient,
        content
      )
    } yield messageId

    query.transact(database).unsafeRunSync()
  }

  protected def createExampleMessages(
      recipientId: ParticipantId
  ): Seq[(MessageId, ConnectionId)] = {
    val acceptedConnections = for (_ <- 1 to 6) yield {
      val issuer = createIssuer(s"Issuer-${randomId()}")
      (createConnection(issuer, recipientId), issuer)
    }

    val initiatedConnections = for (_ <- 1 to 2) yield {
      val holder = createHolder(s"Holder-${randomId()}")
      (createConnection(recipientId, holder), holder)
    }

    val connections = (acceptedConnections ++ initiatedConnections)

    for (i <- 1 to 30) yield {
      val (connection, sender) = connections((i - 1) % connections.size)
      // Add time padding to make sure that two messages are not created at the exact same timestamp
      Thread.sleep(10)
      (
        createMessage(sender, connection, s"message-${randomId()}".getBytes),
        connection
      )
    }
  }

  private def randomId(): String = UUID.randomUUID().toString
}
