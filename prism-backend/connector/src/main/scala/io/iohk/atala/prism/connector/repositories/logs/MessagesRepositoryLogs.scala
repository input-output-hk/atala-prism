package io.iohk.atala.prism.connector.repositories.logs

import cats.data.NonEmptyList
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.model.{ConnectionId, Message, MessageId}
import io.iohk.atala.prism.connector.repositories.MessagesRepository
import io.iohk.atala.prism.connector.repositories.MessagesRepository._
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

private[repositories] final class MessagesRepositoryLogs[S[_], F[
    _
]: ServiceLogging[*[_], MessagesRepository[S, F]]](implicit
    monadThrow: MonadThrow[F]
) extends MessagesRepository[S, Mid[F, *]] {
  override def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId]
  ): Mid[F, Either[InsertMessageError, MessageId]] =
    in =>
      info"inserting message $sender $connection" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while inserting message ${er.unify: ConnectorError}",
            id => info"inserting message - successfully done $id"
          )
        )
        .onError(errorCause"encountered an error while inserting message" (_))

  override def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): Mid[F, Either[InsertMessagesError, List[MessageId]]] =
    in =>
      info"inserting messages $sender" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while inserting messages ${er.unify: ConnectorError}",
            ids => info"inserting messages - successfully done, inserted ${ids.size} messages"
          )
        )
        .onError(errorCause"encountered an error while inserting messages" (_))

  override def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): Mid[F, Either[GetMessagesPaginatedError, List[Message]]] =
    in =>
      info"getting messages paginated $recipientId $lastSeenMessageId" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while getting messages paginated ${er.unify: ConnectorError}",
            ids => info"getting messages paginated - successfully done, got ${ids.size} messages"
          )
        )
        .onError(
          errorCause"encountered an error while getting messages paginated" (_)
        )

  // Won't be called since not mid
  override def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): S[Message] =
    this.getMessageStream(recipientId, lastSeenMessageId)

  override def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): Mid[F, List[Message]] =
    in =>
      info"getting connection messages $recipientId $connectionId" *> in
        .flatTap(result => info"getting connection messages - successfully done, got ${result.size} entities")
        .onError(
          errorCause"encountered an error while getting connection messages" (_)
        )
}
