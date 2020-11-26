package io.iohk.atala.prism.connector.services

import cats.effect.IO
import fs2.Stream
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.MessagesRepository
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither

class MessagesService(messagesRepository: MessagesRepository) {
  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte]
  ): FutureEither[Nothing, MessageId] = {
    messagesRepository.insertMessage(sender, connection, content)
  }

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): FutureEither[ConnectorError, Seq[Message]] = {
    messagesRepository.getMessagesPaginated(recipientId, limit, lastSeenMessageId)
  }

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): Stream[IO, Message] = {
    messagesRepository.getMessageStream(recipientId, lastSeenMessageId)
  }

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): FutureEither[ConnectorError, Seq[Message]] = {
    messagesRepository.getConnectionMessages(recipientId, connectionId)
  }
}
