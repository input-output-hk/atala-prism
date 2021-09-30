package io.iohk.atala.prism.connector.services

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxEitherId
import fs2.Stream
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.repositories.MessagesRepository
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

class MessagesService(messagesRepository: MessagesRepository[Stream[IOWithTraceIdContext, *], IOWithTraceIdContext]) {
  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageId: Option[MessageId] = None
  ): FutureEither[ConnectorError, MessageId] =
    messagesRepository
      .insertMessage(sender, connection, content, messageId)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .toFutureEither

  def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): FutureEither[ConnectorError, List[MessageId]] =
    messagesRepository.insertMessages(sender, messages).run(TraceId.generateYOLO).unsafeToFuture().toFutureEither

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): FutureEither[ConnectorError, Seq[Message]] =
    messagesRepository
      .getMessagesPaginated(recipientId, limit, lastSeenMessageId)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .toFutureEither

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): Stream[IOWithTraceIdContext, Message] =
    messagesRepository.getMessageStream(recipientId, lastSeenMessageId)

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): FutureEither[ConnectorError, Seq[Message]] =
    messagesRepository
      .getConnectionMessages(recipientId, connectionId)
      .map(_.asRight)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .toFutureEither
}
