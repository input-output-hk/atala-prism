package io.iohk.atala.mirror.stubs

import scala.concurrent.duration.FiniteDuration

import fs2.Stream

import io.iohk.atala.mirror.models.{Connection, CredentialProofRequestType, UserCredential}
import io.iohk.atala.mirror.services.ConnectorClientService
import io.iohk.atala.prism.protos.connector_api.{
  GenerateConnectionTokenResponse,
  GetConnectionsPaginatedResponse,
  GetMessagesPaginatedResponse,
  SendMessageResponse
}
import io.iohk.atala.prism.protos.connector_models.{ConnectionInfo, ReceivedMessage}
import monix.eval.Task

class ConnectorClientServiceStub(
    connectionToken: String = "token",
    receivedMessages: Seq[ReceivedMessage] = Nil,
    connectionInfos: Seq[ConnectionInfo] = Nil
) extends ConnectorClientService {

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

  def getConnectionsPaginated(
      lastSeenConnectionId: Option[Connection.ConnectionId],
      limit: Int
  ): Task[GetConnectionsPaginatedResponse] = Task.pure(GetConnectionsPaginatedResponse(connectionInfos))

  def getConnectionsPaginatedStream(
      lastSeenConnectionId: Option[Connection.ConnectionId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, ConnectionInfo] = Stream.emits(connectionInfos)

}
