package io.iohk.atala.prism.connector.services

import cats.{Comonad, Functor}
import cats.data.NonEmptyList
import cats.effect.MonadThrow
import cats.tagless.ApplyK
import cats.syntax.comonad._
import cats.syntax.functor._
import fs2.Stream
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.repositories.MessagesRepository
import io.iohk.atala.prism.connector.services.logs.MessagesServiceLogs
import io.iohk.atala.prism.connector.repositories.MessagesRepository.{
  GetMessagesPaginatedError,
  InsertMessageError,
  InsertMessagesError
}
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}

trait MessagesService[S[_], F[_]] {
  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageId: Option[MessageId] = None
  ): F[Either[InsertMessageError, MessageId]]

  def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): F[Either[InsertMessagesError, List[MessageId]]]

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): F[Either[GetMessagesPaginatedError, List[Message]]]

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): S[Message]

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): F[List[Message]]

}

object MessagesService {
  implicit def applyK[E[_]]: ApplyK[MessagesService[E, *[_]]] =
    cats.tagless.Derive.applyK[MessagesService[E, *[_]]]

  def apply[F[_]: MonadThrow, R[_]: Functor](
      messagesRepository: MessagesRepository[Stream[F, *], F],
      logs: Logs[R, F]
  ): R[MessagesService[Stream[F, *], F]] =
    for {
      serviceLogs <- logs.service[MessagesService[Stream[F, *], F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, MessagesService[Stream[F, *], F]] = serviceLogs
      val logs: MessagesService[Stream[F, *], Mid[F, *]] =
        new MessagesServiceLogs[Stream[F, *], F]
      val mid = logs
      mid attach new MessagesServiceImpl[Stream[F, *], F](messagesRepository)
    }

  def unsafe[F[_]: MonadThrow, R[_]: Comonad](
      messagesRepository: MessagesRepository[Stream[F, *], F],
      logs: Logs[R, F]
  ): MessagesService[Stream[F, *], F] =
    MessagesService(messagesRepository, logs).extract
}

private class MessagesServiceImpl[S[_], F[_]](
    messagesRepository: MessagesRepository[S, F]
) extends MessagesService[S, F] {
  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageId: Option[MessageId] = None
  ): F[Either[InsertMessageError, MessageId]] =
    messagesRepository.insertMessage(sender, connection, content, messageId)

  def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): F[Either[InsertMessagesError, List[MessageId]]] =
    messagesRepository.insertMessages(sender, messages)

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): F[Either[GetMessagesPaginatedError, List[Message]]] =
    messagesRepository.getMessagesPaginated(
      recipientId,
      limit,
      lastSeenMessageId
    )

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): S[Message] =
    messagesRepository.getMessageStream(recipientId, lastSeenMessageId)

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): F[List[Message]] =
    messagesRepository.getConnectionMessages(recipientId, connectionId)
}
