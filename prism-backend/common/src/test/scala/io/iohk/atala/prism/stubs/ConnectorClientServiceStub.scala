package io.iohk.atala.prism.stubs

import scala.concurrent.duration.FiniteDuration
import fs2.Stream
import io.iohk.atala.prism.models.{ConnectionId, ConnectionToken, ConnectorMessageId, CredentialProofRequestType}
import io.iohk.atala.prism.services.ConnectorClientService
import io.iohk.atala.prism.protos.connector_api.{
  GenerateConnectionTokenResponse,
  GetConnectionsPaginatedResponse,
  GetMessagesPaginatedResponse,
  SendMessageRequest,
  SendMessageResponse
}
import io.iohk.atala.prism.protos.connector_models.{ConnectionInfo, ReceivedMessage}
import monix.eval.Task

import java.util.concurrent.atomic.AtomicInteger

class ConnectorClientServiceStub(
    connectionToken: String = "token",
    receivedMessages: Seq[ReceivedMessage] = Nil,
    connectionInfos: Seq[ConnectionInfo] = Nil,
    messageResponse: Task[SendMessageResponse] = Task.pure(SendMessageResponse())
) extends ConnectorClientService {

  var sendMessageInvokeCount: AtomicInteger = new AtomicInteger(0)

  def sendMessage(message: SendMessageRequest): Task[SendMessageResponse] = {
    sendMessageInvokeCount.incrementAndGet()
    messageResponse
  }

  def generateConnectionToken: Task[GenerateConnectionTokenResponse] =
    Task.pure(GenerateConnectionTokenResponse(List(connectionToken)))

  def requestCredential(
      connectionId: ConnectionId,
      connectionToken: ConnectionToken,
      credentialProofRequestTypes: Seq[CredentialProofRequestType]
  ): Task[SendMessageResponse] = Task.pure(SendMessageResponse())

  def getMessagesPaginated(
      lastSeenMessageId: Option[ConnectorMessageId],
      limit: Int
  ): Task[GetMessagesPaginatedResponse] = Task.pure(GetMessagesPaginatedResponse(receivedMessages))

  def getMessagesPaginatedStream(
      lastSeenMessageId: Option[ConnectorMessageId]
  ): Stream[Task, ReceivedMessage] = Stream.emits(receivedMessages)

  def getConnectionsPaginated(
      lastSeenConnectionId: Option[ConnectionId],
      limit: Int
  ): Task[GetConnectionsPaginatedResponse] = Task.pure(GetConnectionsPaginatedResponse(connectionInfos))

  def getConnectionsPaginatedStream(
      lastSeenConnectionId: Option[ConnectionId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, ConnectionInfo] = Stream.emits(connectionInfos)

}
