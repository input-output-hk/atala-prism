package io.iohk.connector.services

import java.time.Instant

import io.iohk.connector.model._
import io.iohk.connector.repositories.MessagesRepository
import io.iohk.cvp.utils.FutureEither

class MessagesService(messagesRepository: MessagesRepository) {
  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte]
  ): FutureEither[Nothing, MessageId] = {
    messagesRepository.insertMessage(sender, connection, content)
  }

  def getMessagesSince(recipientId: ParticipantId, since: Instant, limit: Int): FutureEither[Nothing, Seq[Message]] = {
    messagesRepository.getMessagesSince(recipientId, since, limit)
  }
}
