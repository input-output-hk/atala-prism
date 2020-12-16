package io.iohk.atala.prism.stubs

import scala.concurrent.duration.FiniteDuration
import fs2.Stream
import io.iohk.atala.prism.models.{ConnectionId, ConnectionToken, ConnectorMessageId, CredentialProofRequestType}
import io.iohk.atala.prism.services.ConnectorClientService
import io.iohk.atala.prism.protos.connector_api.{
  GenerateConnectionTokenResponse,
  GetConnectionsPaginatedResponse,
  GetMessagesPaginatedResponse,
  SendMessageResponse
}
import io.iohk.atala.prism.protos.connector_models.{ConnectionInfo, ReceivedMessage}
import io.iohk.atala.prism.protos.credential_models.StartAcuantProcess
import monix.eval.Task

class ConnectorClientServiceStub(
    connectionToken: String = "token",
    receivedMessages: Seq[ReceivedMessage] = Nil,
    connectionInfos: Seq[ConnectionInfo] = Nil
) extends ConnectorClientService {

  def generateConnectionToken: Task[GenerateConnectionTokenResponse] =
    Task.pure(GenerateConnectionTokenResponse(connectionToken))

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
      lastSeenMessageId: Option[ConnectorMessageId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, Seq[ReceivedMessage]] = Stream(receivedMessages)

  def getConnectionsPaginated(
      lastSeenConnectionId: Option[ConnectionId],
      limit: Int
  ): Task[GetConnectionsPaginatedResponse] = Task.pure(GetConnectionsPaginatedResponse(connectionInfos))

  def getConnectionsPaginatedStream(
      lastSeenConnectionId: Option[ConnectionId],
      limit: Int,
      awakeDelay: FiniteDuration
  ): Stream[Task, ConnectionInfo] = Stream.emits(connectionInfos)

  def sendStartAcuantProcess(
      connectionId: ConnectionId,
      startAcuantProcess: StartAcuantProcess
  ): Task[SendMessageResponse] = Task.pure(SendMessageResponse())

}
