package io.iohk.atala.prism.connector.repositories

import cats.data.{EitherT, NonEmptyList}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.traverse._
import cats.effect.{Bracket, BracketThrow}
import cats.tagless.ApplyK
import doobie.ConnectionIO
import doobie.implicits._
import fs2.Stream
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.repositories.daos.{ConnectionsDAO, MessagesDAO}
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

import java.time.Instant

// S - Stream, needed different type because we don't want to have mid for a stream
trait MessagesRepository[S[_], F[_]] {

  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId] = None
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

  def getMessageStream(recipientId: ParticipantId, lastSeenMessageId: Option[MessageId]): S[Message]

  def getConnectionMessages(recipientId: ParticipantId, connectionId: ConnectionId): F[List[Message]]

}

object MessagesRepository {

  implicit def applyK[E[_]]: ApplyK[MessagesRepository[E, *[_]]] =
    cats.tagless.Derive.applyK[MessagesRepository[E, *[_]]]

  def apply[F[_]: TimeMeasureMetric: BracketThrow, G[_]](
      transactor: Transactor[F]
  ): MessagesRepository[Stream[F, *], F] = {
    val metrics: MessagesRepository[Stream[F, *], Mid[F, *]] = new MessagesRepositoryMetrics[F, Stream[F, *]]

    metrics attach new MessagesRepositoryImpl(transactor)
  }
}

private final class MessagesRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends MessagesRepository[Stream[F, *], F]
    with ConnectorErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId] = None
  ): F[Either[ConnectorError, MessageId]] = {
    val messageId = messageIdOption.getOrElse(MessageId.random())

    val query = for {
      _ <- assertUserProvidedIdsNotExist(messageIdOption.toList)

      recipientOption <- EitherT.liftF(ConnectionsDAO.getOtherSide(connection, sender))

      recipient <-
        recipientOption
          .toRight(ConnectionNotFoundByConnectionIdAndSender(sender, connection))
          .toEitherT[ConnectionIO]

      _ <- EitherT.liftF[ConnectionIO, ConnectorError, Unit](
        MessagesDAO.insert(messageId, connection, sender, recipient, content)
      )
    } yield messageId

    query.value
      .logSQLErrors(s"insert messages, connection id - $connection", logger)
      .transact(xa)
  }

  def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): F[Either[ConnectorError, List[MessageId]]] = {
    val connectionTokens = messages.map(_.connectionToken)

    val query = for {
      _ <- assertUserProvidedIdsNotExist(messages.toList.flatMap(_.id))

      otherSidesAndIds <- EitherT.liftF(ConnectionsDAO.getOtherSideAndIdByConnectionTokens(connectionTokens, sender))
      otherSidesAndIdsMap = otherSidesAndIds.map { case (token, id, participant) => token -> (id -> participant) }.toMap

      messagesToInsert <- EitherT.fromEither[ConnectionIO](
        messages
          .map { message =>
            val token = message.connectionToken
            otherSidesAndIdsMap
              .get(token)
              .fold[Either[ConnectorError, CreateMessage]](Left(ConnectionNotFound(token))) {
                case (connectionId, recipientId) =>
                  Right(
                    CreateMessage(
                      id = message.id.getOrElse(MessageId.random()),
                      connection = connectionId,
                      sender = sender,
                      recipient = recipientId,
                      receivedAt = Instant.now(),
                      content = message.message
                    )
                  )
              }
          }
          .toList
          .sequence
      )

      _ <- EitherT.liftF[doobie.ConnectionIO, ConnectorError, Unit](MessagesDAO.insert(messagesToInsert))
    } yield messagesToInsert.map(_.id)

    query
      .transact(xa)
      .value
  }

  private def assertUserProvidedIdsNotExist(ids: List[MessageId]): EitherT[ConnectionIO, ConnectorError, Unit] = {
    val distinctIds = ids.distinct
    for {
      _ <-
        Either
          .cond(
            test = distinctIds.size == ids.size,
            right = (),
            left = MessageIdsNotUnique(ids.diff(distinctIds))
          )
          .toEitherT[ConnectionIO]

      alreadyExistingMessages <- ids match {
        case head :: tail => EitherT.liftF(MessagesDAO.getIdsOfAlreadyExistingMessages(NonEmptyList.of(head, tail: _*)))
        case Nil => EitherT.liftF(doobie.free.connection.pure(List.empty[MessageId]))
      }

      _ <-
        Either
          .cond[ConnectorError, Unit](
            test = alreadyExistingMessages.isEmpty,
            right = (),
            left = MessagesAlreadyExist(alreadyExistingMessages)
          )
          .toEitherT[ConnectionIO]
    } yield ()
  }

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): F[Either[ConnectorError, List[Message]]] = {
    implicit val loggingContext: LoggingContext = LoggingContext(
      "recipientId" -> recipientId,
      "limit" -> limit,
      "lastSeenMessageId" -> lastSeenMessageId
    )

    if (limit <= 0)
      InvalidArgumentError("limit", "positive value", limit.toString).logWarn.asLeft[List[Message]].pure[F]
    else
      MessagesDAO
        .getMessagesPaginated(recipientId, limit, lastSeenMessageId)
        .logSQLErrors(s"getting messages paginated, recipient id - $recipientId", logger)
        .map(_.asRight[ConnectorError])
        .transact(xa)
  }

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): Stream[F, Message] = MessagesDAO.getMessageStream(recipientId, lastSeenMessageId).transact(xa)

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): F[List[Message]] =
    MessagesDAO
      .getConnectionMessages(recipientId, connectionId)
      .logSQLErrors(s"getting connection messages, connection id - $connectionId", logger)
      .transact(xa)
}

private final class MessagesRepositoryMetrics[F[_]: TimeMeasureMetric, S[_]](implicit br: Bracket[F, Throwable])
    extends MessagesRepository[S, Mid[F, *]] {

  private val repoName = "MessagesRepository"
  private lazy val insertMessageTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "insertMessage")
  private lazy val insertMessagesTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "upsertMany")
  private lazy val getMessagesPaginatedTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "getMessagesPaginated")
  private lazy val getConnectionMessagesTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getConnectionMessages")

  override def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId]
  ): Mid[F, Either[ConnectorError, MessageId]] = _.measureOperationTime(insertMessageTimer)

  override def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): Mid[F, Either[ConnectorError, List[MessageId]]] = _.measureOperationTime(insertMessagesTimer)

  override def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): Mid[F, Either[ConnectorError, List[Message]]] = _.measureOperationTime(getMessagesPaginatedTimer)

  override def getConnectionMessages(recipientId: ParticipantId, connectionId: ConnectionId): Mid[F, List[Message]] =
    _.measureOperationTime(getConnectionMessagesTimer)

  // Won't be called since it's not mid but S[_]
  override def getMessageStream(recipientId: ParticipantId, lastSeenMessageId: Option[MessageId]): S[Message] =
    ???
}
