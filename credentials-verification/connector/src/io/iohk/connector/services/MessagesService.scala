package io.iohk.connector.services

import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.connector.errors.ConnectorError
import io.iohk.connector.model._
import io.iohk.connector.repositories.MessagesRepository

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

  def getMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): FutureEither[ConnectorError, Seq[Message]] = {

    messagesRepository.getMessages(recipientId, connectionId)
  }
}
