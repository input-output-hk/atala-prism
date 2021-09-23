package io.iohk.atala.prism.connector.services

import cats.data.NonEmptyList
import cats.tagless.ApplyK
import fs2.Stream
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.repositories.MessagesRepository
import io.iohk.atala.prism.models.ParticipantId

trait MessagesService[S[_], F[_]] {
  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageId: Option[MessageId] = None
  ): F[Either[ConnectorError, MessageId]]

  def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): F[Either[ConnectorError, List[MessageId]]]

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): F[Either[ConnectorError, List[Message]]]

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): Stream[S, Message]

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): F[List[Message]]

}

object MessagesService {
  implicit def applyK[E[_]]: ApplyK[MessagesService[E, *[_]]] = cats.tagless.Derive.applyK[MessagesService[E, *[_]]]

  def apply[S[_], F[_]](messagesRepository: MessagesRepository[Stream[S, *], F]): MessagesService[S, F] =
    new MessagesServiceImpl(messagesRepository)
}

private class MessagesServiceImpl[S[_], F[_]](messagesRepository: MessagesRepository[Stream[S, *], F])
    extends MessagesService[S, F] {
  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageId: Option[MessageId] = None
  ): F[Either[ConnectorError, MessageId]] =
    messagesRepository.insertMessage(sender, connection, content, messageId)

  def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): F[Either[ConnectorError, List[MessageId]]] =
    messagesRepository.insertMessages(sender, messages)

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): F[Either[ConnectorError, List[Message]]] =
    messagesRepository.getMessagesPaginated(recipientId, limit, lastSeenMessageId)

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): Stream[S, Message] =
    messagesRepository.getMessageStream(recipientId, lastSeenMessageId)

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): F[List[Message]] =
    messagesRepository.getConnectionMessages(recipientId, connectionId)
}
