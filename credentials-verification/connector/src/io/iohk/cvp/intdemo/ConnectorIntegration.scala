package io.iohk.cvp.intdemo

import io.iohk.connector.errors.{ConnectorError, ErrorSupport}
import io.iohk.connector.model._
import io.iohk.connector.services.{ConnectionsService, MessagesService}
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.protos.credential_models
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

trait ConnectorIntegration {
  def sendCredential(
      senderId: ParticipantId,
      connectionId: ConnectionId,
      credential: credential_models.Credential
  ): Future[MessageId]

  def sendProofRequest(
      senderId: ParticipantId,
      connectionId: ConnectionId,
      proofRequest: credential_models.ProofRequest
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

    import io.iohk.connector.model.{ConnectionId, TokenString}
    import io.iohk.cvp.models.ParticipantId

    val logger: Logger = LoggerFactory.getLogger(classOf[ConnectorIntegrationImpl])

    override def sendCredential(
        senderId: ParticipantId,
        connectionId: ConnectionId,
        credential: credential_models.Credential
    ): Future[MessageId] = {
      sendMessage(senderId, connectionId, credential.toByteArray)
    }

    override def sendProofRequest(
        senderId: ParticipantId,
        connectionId: ConnectionId,
        proofRequest: credential_models.ProofRequest
    ): Future[MessageId] = {
      sendMessage(senderId, connectionId, proofRequest.toByteArray)
    }

    private def sendMessage(
        senderId: ParticipantId,
        connectionId: ConnectionId,
        message: Array[Byte]
    ): Future[MessageId] = {
      messagesService
        .insertMessage(senderId, connectionId, message)
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
