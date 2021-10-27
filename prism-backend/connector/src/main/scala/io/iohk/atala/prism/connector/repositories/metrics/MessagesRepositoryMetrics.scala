package io.iohk.atala.prism.connector.repositories.metrics

import cats.data.NonEmptyList
import cats.effect.Bracket
import io.iohk.atala.prism.connector.model.{ConnectionId, Message, MessageId}
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.repositories.MessagesRepository
import io.iohk.atala.prism.connector.repositories.MessagesRepository._
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid

private[repositories] final class MessagesRepositoryMetrics[S[_], F[
    _
]: TimeMeasureMetric](implicit
    br: Bracket[F, Throwable]
) extends MessagesRepository[S, Mid[F, *]] {

  private val repoName = "MessagesRepository"
  private lazy val insertMessageTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "insertMessage")
  private lazy val insertMessagesTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "upsertMany")
  private lazy val getMessagesPaginatedTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getMessagesPaginated")
  private lazy val getConnectionMessagesTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getConnectionMessages")

  override def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId]
  ): Mid[F, Either[InsertMessageError, MessageId]] =
    _.measureOperationTime(insertMessageTimer)

  override def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): Mid[F, Either[InsertMessagesError, List[MessageId]]] =
    _.measureOperationTime(insertMessagesTimer)

  override def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): Mid[F, Either[GetMessagesPaginatedError, List[Message]]] =
    _.measureOperationTime(getMessagesPaginatedTimer)

  override def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): Mid[F, List[Message]] =
    _.measureOperationTime(getConnectionMessagesTimer)

  // Won't be called since it's not mid but S[_]
  override def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): S[Message] =
    ???
}
