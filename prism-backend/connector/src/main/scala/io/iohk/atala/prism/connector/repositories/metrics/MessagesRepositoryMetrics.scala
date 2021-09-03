package io.iohk.atala.prism.connector.repositories.metrics

import cats.data.NonEmptyList
import cats.effect.Bracket
import io.iohk.atala.prism.connector.errors.InvalidArgumentError
import io.iohk.atala.prism.connector.errors.MessagesError._
import io.iohk.atala.prism.connector.model.{ConnectionId, Message, MessageId}
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.repositories.MessagesRepository
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid

private[repositories] final class MessagesRepositoryMetrics[F[_]: TimeMeasureMetric, S[_]](implicit
    br: Bracket[F, Throwable]
) extends MessagesRepository[S, Mid[F, *]] {

  private val repoName = "MessagesRepository"
  private lazy val insertMessageTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "insertMessage")
  private lazy val insertMessagesTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "upsertMany")
  private lazy val getMessagesPaginatedTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "getMessagesPaginated")
  private lazy val getConnectionMessagesTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getConnectionMessages")

  override def insertMessage[E
  : ConnectionNotFound <:< *
  : ConnectionRevoked <:< *
  : ConnectionNotFoundByConnectionIdAndSender <:< *
  : MessagesAlreadyExist <:< *
  : MessageIdsNotUnique <:< *
  ](
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId]
  ): Mid[F, Either[E, MessageId]] = _.measureOperationTime(insertMessageTimer)

  override def insertMessages[E
  : ConnectionNotFound <:< *
  : ConnectionRevoked <:< *
  : MessagesAlreadyExist <:< *
  : MessageIdsNotUnique <:< *
  ](
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): Mid[F, Either[E, List[MessageId]]] = _.measureOperationTime(insertMessagesTimer)

  override def getMessagesPaginated[E : InvalidArgumentError <:< *](
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): Mid[F, Either[E, List[Message]]] = _.measureOperationTime(getMessagesPaginatedTimer)

  override def getConnectionMessages(recipientId: ParticipantId, connectionId: ConnectionId): Mid[F, List[Message]] =
    _.measureOperationTime(getConnectionMessagesTimer)

  // Won't be called since it's not mid but S[_]
  override def getMessageStream(recipientId: ParticipantId, lastSeenMessageId: Option[MessageId]): S[Message] =
    ???
}
