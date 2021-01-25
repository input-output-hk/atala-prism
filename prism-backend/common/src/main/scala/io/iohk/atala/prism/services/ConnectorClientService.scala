package io.iohk.atala.prism.services

import scala.concurrent.duration.FiniteDuration
import monix.eval.Task
import fs2.Stream
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.protos.connector_api._
import io.iohk.atala.prism.protos.credential_models.{StartAcuantProcess, AtalaMessage, ProofRequest, KycBridgeMessage}
import io.iohk.atala.prism.protos.connector_models.{ConnectionInfo, ReceivedMessage}
import io.iohk.atala.prism.config.ConnectorConfig
import io.iohk.atala.prism.models.{ConnectionId, ConnectionToken}
import io.iohk.atala.prism.models.{ConnectorMessageId, CredentialProofRequestType}

trait ConnectorClientService {

  def generateConnectionToken: Task[GenerateConnectionTokenResponse]

  def sendMessage(
      message: SendMessageRequest
  ): Task[SendMessageResponse]

  def requestCredential(
      connectionId: ConnectionId,
      connectionToken: ConnectionToken,
      credentialProofRequestTypes: Seq[CredentialProofRequestType]
  ): Task[SendMessageResponse]

  def getMessagesPaginated(
      lastSeenMessageId: Option[ConnectorMessageId],
      limit: Int
  ): Task[GetMessagesPaginatedResponse]

  def getMessagesPaginatedStream(
      lastSeenMessageId: Option[ConnectorMessageId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, Seq[ReceivedMessage]]

  def getConnectionsPaginated(
      lastSeenConnectionId: Option[ConnectionId],
      limit: Int
  ): Task[GetConnectionsPaginatedResponse]

  def getConnectionsPaginatedStream(
      lastSeenConnectionId: Option[ConnectionId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, ConnectionInfo]

  def sendStartAcuantProcess(
      connectionId: ConnectionId,
      startAcuantProcess: StartAcuantProcess
  ): Task[SendMessageResponse]

}

class ConnectorClientServiceImpl(
    connector: ConnectorServiceGrpc.ConnectorServiceStub,
    requestAuthenticator: RequestAuthenticator,
    connectorConfig: ConnectorConfig
) extends BaseGrpcClientService(connector, requestAuthenticator, connectorConfig.authConfig)
    with ConnectorClientService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def sendMessage(message: SendMessageRequest): Task[SendMessageResponse] =
    authenticatedCall(message, _.sendMessage)

  def generateConnectionToken: Task[GenerateConnectionTokenResponse] =
    authenticatedCall(GenerateConnectionTokenRequest(), _.generateConnectionToken)

  def requestCredential(
      connectionId: ConnectionId,
      connectionToken: ConnectionToken,
      credentialProofRequestTypes: Seq[CredentialProofRequestType]
  ): Task[SendMessageResponse] = {
    val proofRequest =
      AtalaMessage().withProofRequest(ProofRequest(credentialProofRequestTypes.map(_.typeId), connectionToken.token))

    val request = SendMessageRequest(connectionId.uuid.toString, proofRequest.toByteString)

    authenticatedCall(request, _.sendMessage)
  }

  def getMessagesPaginated(
      lastSeenMessageId: Option[ConnectorMessageId],
      limit: Int
  ): Task[GetMessagesPaginatedResponse] = {
    val request = GetMessagesPaginatedRequest(lastSeenMessageId.map(_.messageId).getOrElse(""), limit)

    authenticatedCall(request, _.getMessagesPaginated)
  }

  def getMessagesPaginatedStream(
      lastSeenMessageId: Option[ConnectorMessageId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, Seq[ReceivedMessage]] = {
    val initialAwakeDelay = false
    Stream
      .unfoldEval[Task, (Option[ConnectorMessageId], Boolean), Seq[ReceivedMessage]](
        (lastSeenMessageId, initialAwakeDelay)
      ) {
        case (lastSeenMessageId, shouldApplyAwakeDelay) =>
          getMessagesPaginated(lastSeenMessageId, limit).flatMap(response =>
            for {
              _ <- if (shouldApplyAwakeDelay) Task.sleep(awakeDelay) else Task.unit
              _ = logger.info(s"Call GetMessagesPaginated - lastSeenMessageId: ${lastSeenMessageId}")

              result = response.messages match {
                case Nil =>
                  val applyAwakeDelay = true
                  Some(Nil -> (lastSeenMessageId -> applyAwakeDelay))
                case messages =>
                  val applyAwakeDelay = messages.size != limit
                  Some(messages -> (Some(ConnectorMessageId(messages.last.id)) -> applyAwakeDelay))
              }
            } yield result
          )
      }
  }

  def getConnectionsPaginated(
      lastSeenConnectionId: Option[ConnectionId],
      limit: Int
  ): Task[GetConnectionsPaginatedResponse] = {
    val request = GetConnectionsPaginatedRequest(lastSeenConnectionId.map(_.uuid.toString).getOrElse(""), limit)

    authenticatedCall(request, _.getConnectionsPaginated)
  }

  /**
    * Create a continuous stream with subsequent calls to connector's GetConnectionsPaginated method.
    *
    * @param lastSeenConnectionId initial connection id
    * @param limit limit of connections per one request to the gRPC
    * @param awakeDelay the delay between each call to the connector
    */
  def getConnectionsPaginatedStream(
      lastSeenConnectionId: Option[ConnectionId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, ConnectionInfo] = {
    val initialAwakeDelay = false
    Stream
      .unfoldEval[Task, (Option[ConnectionId], Boolean), Seq[ConnectionInfo]](
        (lastSeenConnectionId, initialAwakeDelay)
      ) {
        case (lastSeenConnectionId, shouldApplyAwakeDelay) =>
          getConnectionsPaginated(lastSeenConnectionId, limit).flatMap(response =>
            for {
              _ <- if (shouldApplyAwakeDelay) Task.sleep(awakeDelay) else Task.unit
              _ = logger.info(s"Call GetConnectionsPaginated - lastSeenConnectionId: ${lastSeenConnectionId}")

              result = response.connections match {
                case Nil =>
                  val applyAwakeDelay = true
                  Some(Nil -> (lastSeenConnectionId -> applyAwakeDelay))
                case connections =>
                  val applyAwakeDelay = connections.size != limit
                  Some(
                    connections -> (ConnectionId.from(connections.last.connectionId).toOption -> applyAwakeDelay)
                  )
              }
            } yield result
          )
      }
      .flatMap(Stream.emits) // convert Seq[ConnectionInfo] to ConnectionInfo
  }

  def sendStartAcuantProcess(
      connectionId: ConnectionId,
      startAcuantProcess: StartAcuantProcess
  ): Task[SendMessageResponse] = {
    val kycBridgeMessage =
      AtalaMessage().withKycBridgeMessage(
        KycBridgeMessage(KycBridgeMessage.Message.StartAcuantProcess(startAcuantProcess))
      )

    val request = SendMessageRequest(connectionId.uuid.toString, kycBridgeMessage.toByteString)

    authenticatedCall(request, _.sendMessage)
  }

}

object ConnectorClientService {

  def createConnectorGrpcStub(
      connectorConfig: ConnectorConfig
  ): ConnectorServiceGrpc.ConnectorServiceStub =
    createConnectorGrpcStub(connectorConfig.host, connectorConfig.port)

  def createConnectorGrpcStub(
      host: String,
      port: Int
  ): ConnectorServiceGrpc.ConnectorServiceStub = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build()

    ConnectorServiceGrpc.stub(channel)
  }
}
