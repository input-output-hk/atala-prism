package io.iohk.atala.prism.intdemo

import io.iohk.atala.prism.connector.errors.{ConnectorError, ConnectorErrorSupport, MessagesError}
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.services.{ConnectionsService, MessagesService}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

trait ConnectorIntegration {
  def sendCredential(
      senderId: ParticipantId,
      connectionId: ConnectionId,
      credential: credential_models.PlainTextCredential
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

  class ConnectorIntegrationImpl(connectionsService: ConnectionsService, messagesService: MessagesService)(implicit
      ec: ExecutionContext
  ) extends ConnectorIntegration
      with ConnectorErrorSupport {

    import io.iohk.atala.prism.connector.model.{ConnectionId, TokenString}
    import io.iohk.atala.prism.models.ParticipantId

    val logger: Logger = LoggerFactory.getLogger(classOf[ConnectorIntegrationImpl])

    override def sendCredential(
        senderId: ParticipantId,
        connectionId: ConnectionId,
        credential: credential_models.PlainTextCredential
    ): Future[MessageId] = {
      sendMessage(
        senderId,
        connectionId,
        AtalaMessage().withPlainCredential(credential).toByteArray
      )
    }

    override def sendProofRequest(
        senderId: ParticipantId,
        connectionId: ConnectionId,
        proofRequest: credential_models.ProofRequest
    ): Future[MessageId] = {
      sendMessage(senderId, connectionId, AtalaMessage().withProofRequest(proofRequest).toByteArray)
    }

    private def sendMessage(
        senderId: ParticipantId,
        connectionId: ConnectionId,
        message: Array[Byte]
    ): Future[MessageId] = {
      messagesService
        .insertMessage[MessagesError](senderId, connectionId, message)
        .toFuture(toRuntimeException(senderId, connectionId))
    }

    override def getConnectionByToken(token: TokenString): Future[Option[Connection]] = {
      connectionsService
        .getConnectionByToken(token)
        .toFuture(toRuntimeException)
    }

    override def generateConnectionToken(senderId: ParticipantId): Future[TokenString] = {
      connectionsService
        .generateTokens(senderId, tokensCount = 1)
        .toFuture(toRuntimeException)
        .flatMap(
          _.headOption
            .map(Future.successful)
            .getOrElse(Future.failed(new RuntimeException("Connection service returned empty connection tokens list")))
        )
    }

    override def getMessages(
        recipientId: ParticipantId,
        connectionId: ConnectionId
    ): Future[Seq[Message]] = {
      messagesService
        .getConnectionMessages(recipientId, connectionId)
        .toFuture(toRuntimeException)
    }

    private def toRuntimeException(senderId: ParticipantId, connectionId: ConnectionId): Any => RuntimeException =
      _ => new RuntimeException(s"Failed to send credential for senderId = $senderId, connectionId = $connectionId.")

    private def toRuntimeException: ConnectorError => RuntimeException =
      error =>
        new RuntimeException(s"Failed to get connection due to connector error: '${error.toStatus.getDescription}'.")
  }
}
