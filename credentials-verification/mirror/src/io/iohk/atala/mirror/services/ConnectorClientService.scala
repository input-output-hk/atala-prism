package io.iohk.atala.mirror.services

import monix.eval.Task
import io.iohk.prism.protos.connector_api._
import io.iohk.atala.requests.RequestAuthenticator
import io.iohk.atala.mirror.config.ConnectorConfig
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken}
import io.iohk.atala.mirror.models.CredentialProofRequestType
import io.iohk.atala.mirror.models.UserCredential.MessageId
import io.iohk.prism.protos.connector_models.ReceivedMessage
import io.iohk.prism.protos.credential_models.{AtalaMessage, ProofRequest}
import fs2.Stream
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration

trait ConnectorClientService {

  def generateConnectionToken: Task[GenerateConnectionTokenResponse]

  def requestCredential(
      connectionId: ConnectionId,
      connectionToken: ConnectionToken,
      credentialProofRequestTypes: Seq[CredentialProofRequestType]
  ): Task[SendMessageResponse]

  def getMessagesPaginated(
      lastSeenMessageId: Option[MessageId],
      limit: Int
  ): Task[GetMessagesPaginatedResponse]

  def getMessagesPaginatedStream(
      lastSeenMessageId: Option[MessageId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, Seq[ReceivedMessage]]

}

class ConnectorClientServiceImpl(
    connector: ConnectorServiceGrpc.ConnectorServiceStub,
    requestAuthenticator: RequestAuthenticator,
    connectorConfig: ConnectorConfig
) extends BaseGrpcClientService(connector, requestAuthenticator, connectorConfig.authConfig)
    with ConnectorClientService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def generateConnectionToken: Task[GenerateConnectionTokenResponse] = {
    val request = GenerateConnectionTokenRequest()

    authenticatedCall(request, _.generateConnectionToken)
  }

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
      lastSeenMessageId: Option[MessageId],
      limit: Int
  ): Task[GetMessagesPaginatedResponse] = {
    val request = GetMessagesPaginatedRequest(lastSeenMessageId.map(_.messageId).getOrElse(""), limit)

    authenticatedCall(request, _.getMessagesPaginated)
  }

  def getMessagesPaginatedStream(
      lastSeenMessageId: Option[MessageId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, Seq[ReceivedMessage]] = {
    val initialAwakeDelay = false
    Stream
      .unfoldEval[Task, (Option[MessageId], Boolean), Seq[ReceivedMessage]](
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
                  Some(messages -> (Some(MessageId(messages.last.id)) -> applyAwakeDelay))
              }
            } yield result
          )
      }
  }
}
