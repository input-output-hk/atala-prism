package io.iohk.atala.prism.connector.services.logs

import cats.data.NonEmptyList
import cats.effect.MonadThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.model.{ConnectionId, Message, MessageId}
import io.iohk.atala.prism.connector.repositories.MessagesRepository.{
  GetMessagesPaginatedError,
  InsertMessageError,
  InsertMessagesError
}
import io.iohk.atala.prism.connector.services.MessagesService
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

class MessagesServiceLogs[S[_], F[_]: ServiceLogging[
  *[_],
  MessagesService[S, F]
]: MonadThrow]
    extends MessagesService[S, Mid[F, *]] {
  override def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageId: Option[MessageId]
  ): Mid[F, Either[InsertMessageError, MessageId]] =
    in =>
      info"Inserting message $sender $connection" *>
        in.flatTap(
          _.fold(
            e => error"Encountered an error while inserting message ${e.unify: ConnectorError}",
            messageId => info"Inserting message - successfully done $messageId"
          )
        ).onError(errorCause"Encountered an error while inserting message" (_))

  override def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): Mid[F, Either[InsertMessagesError, List[MessageId]]] =
    in =>
      info"Inserting messages $sender ${messages.size}" *>
        in.flatTap(
          _.fold(
            e => error"Encountered an error while inserting messages ${e.unify: ConnectorError}",
            result => info"Inserting messages - successfully done, inserted ${result.size} messages"
          )
        ).onError(errorCause"Encountered an error while inserting message" (_))

  override def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): Mid[F, Either[GetMessagesPaginatedError, List[Message]]] =
    in =>
      info"Getting messages paginated $recipientId" *>
        in.flatTap(
          _.fold(
            e => error"Encountered an error while getting messages paginated ${e.unify: ConnectorError}",
            result => info"Getting messages paginated - successfully done, got ${result.size} messages"
          )
        ).onError(
          errorCause"Encountered an error while getting messages paginated" (_)
        )

  // Wont be called since we use Mid for F only
  override def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): S[Message] = ???

  override def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): Mid[F, List[Message]] =
    in =>
      info"Getting connection messages $recipientId" *>
        in.flatTap(result => info"Getting connection messages - successfully done, got ${result.size} messages")
          .onError(
            errorCause"Encountered an error while getting connection messages" (_)
          )
}
