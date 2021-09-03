package io.iohk.atala.prism.connector.services

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import fs2.Stream
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.errors.MessagesError._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.repositories.MessagesRepository
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

class MessagesService(messagesRepository: MessagesRepository[Stream[IO, *], IO]) {
  def insertMessage[
    E
    : ConnectionNotFound <:< *
    : ConnectionRevoked <:< *
    : ConnectionNotFoundByConnectionIdAndSender <:< *
    : MessagesAlreadyExist <:< *
    : MessageIdsNotUnique <:< *
  ](
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageId: Option[MessageId] = None
  ): FutureEither[E, MessageId] =
    messagesRepository.insertMessage[E](sender, connection, content, messageId).unsafeToFuture().toFutureEither

  def insertMessages[
    E
    : ConnectionNotFound <:< *
    : ConnectionRevoked <:< *
    : MessagesAlreadyExist <:< *
    : MessageIdsNotUnique <:< *
  ](
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): FutureEither[E, List[MessageId]] =
    messagesRepository.insertMessages[E](sender, messages).unsafeToFuture().toFutureEither

  def getMessagesPaginated[E : InvalidLimitError <:< *](
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): FutureEither[E, Seq[Message]] =
    messagesRepository.getMessagesPaginated(recipientId, limit, lastSeenMessageId).unsafeToFuture().toFutureEither

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): Stream[IO, Message] =
    messagesRepository.getMessageStream(recipientId, lastSeenMessageId)

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): FutureEither[ConnectorError, Seq[Message]] =
    messagesRepository.getConnectionMessages(recipientId, connectionId).map(_.asRight).unsafeToFuture().toFutureEither
}
