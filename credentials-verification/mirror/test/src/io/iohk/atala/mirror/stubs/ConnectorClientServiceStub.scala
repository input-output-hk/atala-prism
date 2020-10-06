package io.iohk.atala.mirror.stubs

import fs2.Stream
import io.iohk.atala.mirror.models.{Connection, CredentialProofRequestType, UserCredential}
import io.iohk.atala.mirror.services.ConnectorClientService
import io.iohk.prism.protos.connector_api.{
  GenerateConnectionTokenResponse,
  GetMessagesPaginatedResponse,
  SendMessageResponse
}
import io.iohk.prism.protos.connector_models.ReceivedMessage
import monix.eval.Task

import scala.concurrent.duration.FiniteDuration

class ConnectorClientServiceStub(connectionToken: String = "token", receivedMessages: Seq[ReceivedMessage] = Seq.empty)
    extends ConnectorClientService {

  def generateConnectionToken: Task[GenerateConnectionTokenResponse] =
    Task.pure(GenerateConnectionTokenResponse(connectionToken))

  def requestCredential(
      connectionId: Connection.ConnectionId,
      connectionToken: Connection.ConnectionToken,
      credentialProofRequestTypes: Seq[CredentialProofRequestType]
  ): Task[SendMessageResponse] = Task.pure(SendMessageResponse())

  def getMessagesPaginated(
      lastSeenMessageId: Option[UserCredential.MessageId],
      limit: Int
  ): Task[GetMessagesPaginatedResponse] = Task.pure(GetMessagesPaginatedResponse(receivedMessages))

  def getMessagesPaginatedStream(
      lastSeenMessageId: Option[UserCredential.MessageId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, Seq[ReceivedMessage]] = Stream(receivedMessages)

}
