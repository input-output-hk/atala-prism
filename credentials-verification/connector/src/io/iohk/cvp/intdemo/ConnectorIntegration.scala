package io.iohk.cvp.intdemo

import credential.Credential
import io.iohk.connector.errors.{ConnectorError, ErrorSupport}
import io.iohk.connector.model.{Connection, ConnectionId, Message, MessageId, TokenString}
import io.iohk.connector.services.{ConnectionsService, MessagesService}
import io.iohk.cvp.models.ParticipantId
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

trait ConnectorIntegration {
  def sendCredential(
      senderId: ParticipantId,
      connectionId: ConnectionId,
      credential: Credential
  ): Future[MessageId]

  def getConnectionByToken(token: TokenString): Future[Option[Connection]]

  def generateConnectionToken(senderId: ParticipantId): Future[TokenString]

  def getMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): Future[Seq[Message]]
}

object ConnectorIntegration {

  class ConnectorIntegrationImpl(connectionsService: ConnectionsService, messagesService: MessagesService)(
      implicit ec: ExecutionContext
  ) extends ConnectorIntegration
      with ErrorSupport {

    import io.iohk.cvp.models.ParticipantId
    import io.iohk.connector.model.{ConnectionId, TokenString}

    val logger: Logger = LoggerFactory.getLogger(classOf[ConnectorIntegrationImpl])

    override def sendCredential(
        senderId: ParticipantId,
        connectionId: ConnectionId,
        credential: Credential
    ): Future[MessageId] = {
      messagesService
        .insertMessage(senderId, connectionId, credential.toByteArray)
        .toFuture(toRuntimeException(senderId, connectionId))
    }

    override def getConnectionByToken(token: TokenString): Future[Option[Connection]] = {
      connectionsService
        .getConnectionByToken(token)
        .toFuture(toRuntimeException)
    }

    override def generateConnectionToken(senderId: ParticipantId): Future[TokenString] = {
      connectionsService
        .generateToken(senderId)
        .toFuture(toRuntimeException)
    }

    override def getMessages(
        recipientId: ParticipantId,
        connectionId: ConnectionId
    ): Future[Seq[Message]] = {
      messagesService
        .getMessages(recipientId, connectionId)
        .toFuture(toRuntimeException)
    }

    private def toRuntimeException(senderId: ParticipantId, connectionId: ConnectionId): Nothing => RuntimeException =
      _ => new RuntimeException(s"Failed to send credential for senderId = $senderId, connectionId = $connectionId.")

    private def toRuntimeException: ConnectorError => RuntimeException =
      error =>
        new RuntimeException(s"Failed to get connection due to connector error: '${error.toStatus.getDescription}'.")
  }
}
