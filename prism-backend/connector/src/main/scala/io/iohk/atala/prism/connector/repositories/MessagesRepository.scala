package io.iohk.atala.prism.connector.repositories

import cats.data.{EitherT, NonEmptyList}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.traverse._
import cats.effect.BracketThrow
import cats.tagless.ApplyK
import doobie.ConnectionIO
import doobie.implicits._
import fs2.Stream
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.errors.MessagesError._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.repositories.daos.{ConnectionsDAO, MessagesDAO}
import io.iohk.atala.prism.connector.repositories.metrics.MessagesRepositoryMetrics
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

import java.time.Instant

// S - Stream, needed different type because we don't want to have mid for a stream
trait MessagesRepository[S[_], F[_]] {

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
      messageIdOption: Option[MessageId] = None
  ): F[Either[E, MessageId]]

  def insertMessages[
    E
    : ConnectionNotFound <:< *
    : ConnectionRevoked <:< *
    : MessagesAlreadyExist <:< *
    : MessageIdsNotUnique <:< *
  ](
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): F[Either[E, List[MessageId]]]

  def getMessagesPaginated[E : InvalidLimitError <:< *](
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): F[Either[E, List[Message]]]

  def getMessageStream(recipientId: ParticipantId, lastSeenMessageId: Option[MessageId]): S[Message]

  def getConnectionMessages(recipientId: ParticipantId, connectionId: ConnectionId): F[List[Message]]
}

object MessagesRepository {

  implicit def applyK[S[_]]: ApplyK[MessagesRepository[S, *[_]]] =
    cats.tagless.Derive.applyK[MessagesRepository[S, *[_]]]

  def apply[F[_]: TimeMeasureMetric: BracketThrow, G[_]](
      transactor: Transactor[F]
  ): MessagesRepository[Stream[F, *], F] = {
    val metrics: MessagesRepository[Stream[F, *], Mid[F, *]] = new MessagesRepositoryMetrics[F, Stream[F, *]]

    metrics attach new MessagesRepositoryImpl(transactor)
  }
}

private final class MessagesRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends MessagesRepository[Stream[F, *], F]
    with ConnectorErrorSupportNew {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def insertMessage[E
      : ConnectionNotFound <:< *
      : ConnectionRevoked <:< *
      : ConnectionNotFoundByConnectionIdAndSender <:< *
      : MessagesAlreadyExist <:< *
      : MessageIdsNotUnique <:< *
  ](
      sender: ParticipantId,
      connectionId: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId] = None
  ): F[Either[E, MessageId]] = {
    val messageId = messageIdOption.getOrElse(MessageId.random())

    val query = for {
      _ <- assertUserProvidedIdsNotExist[E](messageIdOption.toList)

      rawConnection <- EitherT[ConnectionIO, E, RawConnection](
        ConnectionsDAO.getRawConnection(connectionId)
        .map(_.toRight(ConnectionNotFound(connectionId))))

      _ <- EitherT.fromEither[ConnectionIO] {
        Either.cond[E, Unit](
          rawConnection.status != ConnectionStatus.ConnectionRevoked,
          right = (),
          left = ConnectionRevoked(connectionId))
      }

      recipientOption <- EitherT.liftF(ConnectionsDAO.getOtherSide(connectionId, sender))

      recipient <-
        recipientOption
          .toRight[E](ConnectionNotFoundByConnectionIdAndSender(sender, connectionId))
          .toEitherT[ConnectionIO]

      _ <- EitherT.liftF[ConnectionIO, E, Unit](
        MessagesDAO.insert(messageId, connectionId, sender, recipient, content)
      )
    } yield messageId

    query.value
      .logSQLErrors(s"insert messages, connection id - $connectionId", logger)
      .transact(xa)
  }

  def insertMessages[
    E
    : ConnectionNotFound <:< *
    : ConnectionRevoked <:< *
    : MessagesAlreadyExist <:< *
    : MessageIdsNotUnique <:< *
  ](
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): F[Either[E, List[MessageId]]] = {
    val connectionTokens = messages.map(_.connectionToken)

    val query = for {
      _ <- assertUserProvidedIdsNotExist[E](messages.toList.flatMap(_.id))

      _ <- EitherT(ConnectionsDAO.getConnectionsByConnectionTokens(connectionTokens.toList)
        .map { connections =>
          connections.find(_.connectionStatus == ConnectionStatus.ConnectionRevoked)
            // We can be sure that a found revoked connection has a corresponding token, so .get is safe
            .toLeft(()).leftMap[E](revoked => ConnectionRevoked(revoked.contactToken.get))
        })

      otherSidesAndIds <- EitherT.liftF(ConnectionsDAO.getOtherSideAndIdByConnectionTokens(connectionTokens, sender))
      otherSidesAndIdsMap = otherSidesAndIds.map { case (token, id, participant) => token -> (id -> participant) }.toMap

      messagesToInsert <- EitherT.fromEither[ConnectionIO](
        messages
          .map { message =>
            val token = message.connectionToken
            otherSidesAndIdsMap
              .get(token)
              .fold[Either[E, CreateMessage]](Left(ConnectionNotFound(token))) {
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

      _ <- EitherT.liftF[ConnectionIO, E, Unit](MessagesDAO.insert(messagesToInsert))
    } yield messagesToInsert.map(_.id)

    query
      .transact(xa)
      .value
  }

  private def assertUserProvidedIdsNotExist[
    E : MessagesAlreadyExist <:< *
      : MessageIdsNotUnique <:< *
  ](ids: List[MessageId]): EitherT[ConnectionIO, E, Unit] = {
    val distinctIds = ids.distinct
    for {
      _ <-
        Either
          .cond[E, Unit](
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
          .cond[E, Unit](
            test = alreadyExistingMessages.isEmpty,
            right = (),
            left = MessagesAlreadyExist(alreadyExistingMessages)
          )
          .toEitherT[ConnectionIO]
    } yield ()
  }

  def getMessagesPaginated[E : InvalidLimitError <:< *](
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): F[Either[E, List[Message]]] = {
    implicit val loggingContext: LoggingContext = LoggingContext(
      "recipientId" -> recipientId,
      "limit" -> limit,
      "lastSeenMessageId" -> lastSeenMessageId
    )

    if (limit <= 0)
      InvalidLimitError(limit.toString)
        .logWarnNew
        .asLeft[List[Message]]
        .leftMap[E](x => x)
        .pure[F]
    else
      MessagesDAO
        .getMessagesPaginated(recipientId, limit, lastSeenMessageId)
        .logSQLErrors(s"getting messages paginated, recipient id - $recipientId", logger)
        .map(_.asRight[E])
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
