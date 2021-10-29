package io.iohk.atala.prism.connector.repositories

import cats.{Applicative, Comonad, Functor}
import cats.data.{EitherT, NonEmptyList}
import cats.syntax.applicative._
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.effect.{BracketThrow, Resource}
import cats.tagless.ApplyK
import doobie.ConnectionIO
import doobie.implicits._
import fs2.Stream
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.errors.MessagesError._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.repositories.MessagesRepository._
import io.iohk.atala.prism.connector.repositories.daos.{ConnectionsDAO, MessagesDAO}
import io.iohk.atala.prism.connector.repositories.logs.MessagesRepositoryLogs
import io.iohk.atala.prism.connector.repositories.metrics.MessagesRepositoryMetrics
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

import java.time.Instant
import shapeless.{:+:, CNil}

// S - Stream, needed different type because we don't want to have mid for a stream
trait MessagesRepository[S[_], F[_]] {

  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId] = None
  ): F[Either[InsertMessageError, MessageId]]

  def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): F[Either[InsertMessagesError, List[MessageId]]]

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): F[Either[GetMessagesPaginatedError, List[Message]]]

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): S[Message]

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): F[List[Message]]
}

object MessagesRepository {

  type InsertMessageError =
    ConnectionNotFound :+:
      ConnectionRevoked :+:
      ConnectionNotFoundByConnectionIdAndSender :+:
      MessagesAlreadyExist :+:
      MessageIdsNotUnique :+:
      CNil

  type InsertMessagesError =
    ConnectionNotFound :+:
      ConnectionRevoked :+:
      MessagesAlreadyExist :+:
      MessageIdsNotUnique :+:
      CNil

  type GetMessagesPaginatedError = InvalidLimitError :+: CNil

  implicit def applyK[E[_]]: ApplyK[MessagesRepository[E, *[_]]] =
    cats.tagless.Derive.applyK[MessagesRepository[E, *[_]]]

  def apply[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[MessagesRepository[Stream[F, *], F]] =
    for {
      serviceLogs <- logs.service[MessagesRepository[Stream[F, *], F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, MessagesRepository[Stream[F, *], F]] = serviceLogs
      val metrics: MessagesRepository[Stream[F, *], Mid[F, *]] =
        new MessagesRepositoryMetrics[Stream[F, *], F]
      val logs: MessagesRepository[Stream[F, *], Mid[F, *]] =
        new MessagesRepositoryLogs[Stream[F, *], F]
      val mid = metrics |+| logs
      mid attach new MessagesRepositoryImpl[F](transactor)
    }

  def resource[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Applicative](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, MessagesRepository[Stream[F, *], F]] =
    Resource.eval(MessagesRepository(transactor, logs))

  def unsafe[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): MessagesRepository[Stream[F, *], F] =
    MessagesRepository(transactor, logs).extract
}

private final class MessagesRepositoryImpl[F[_]: BracketThrow](
    xa: Transactor[F]
) extends MessagesRepository[Stream[F, *], F]
    with ConnectorErrorSupportNew {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def insertMessage(
      sender: ParticipantId,
      connectionId: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId] = None
  ): F[Either[InsertMessageError, MessageId]] = {
    val messageId = messageIdOption.getOrElse(MessageId.random())

    val query: EitherT[ConnectionIO, InsertMessageError, MessageId] = for {
      _ <- assertUserProvidedIdsNotExist(messageIdOption.toList).leftMap(
        _.embed[InsertMessageError]
      )

      rawConnection <- EitherT(
        ConnectionsDAO
          .getRawConnection(connectionId)
          .map(
            _.toRight(co[InsertMessageError](ConnectionNotFound(connectionId)))
          )
      )

      _ <- EitherT.fromEither[ConnectionIO] {
        if (rawConnection.status == ConnectionStatus.ConnectionRevoked)
          Left(co[InsertMessageError](ConnectionRevoked(connectionId)))
        else Right(())
      }

      recipientOption <- EitherT.liftF(
        ConnectionsDAO.getOtherSide(connectionId, sender)
      )

      recipient <-
        recipientOption
          .toRight(
            co[InsertMessageError](
              ConnectionNotFoundByConnectionIdAndSender(sender, connectionId)
            )
          )
          .toEitherT[ConnectionIO]

      _ <- EitherT.liftF(
        MessagesDAO.insert(messageId, connectionId, sender, recipient, content)
      )
    } yield messageId

    query.value
      .logSQLErrors(s"insert messages, connection id - $connectionId", logger)
      .transact(xa)
  }

  def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): F[Either[InsertMessagesError, List[MessageId]]] = {
    val connectionTokens = messages.map(_.connectionToken)

    val query = for {
      _ <- assertUserProvidedIdsNotExist(messages.toList.flatMap(_.id))
        .leftMap(_.embed[InsertMessagesError])

      _ <- EitherT(
        ConnectionsDAO
          .getConnectionsByConnectionTokens(connectionTokens.toList)
          .map { connections =>
            connections
              .find(_.connectionStatus == ConnectionStatus.ConnectionRevoked)
              // We can be sure that a found revoked connection has a corresponding token, so .get is safe
              .toLeft(())
              .leftMap(revoked =>
                co[InsertMessagesError](
                  ConnectionRevoked(revoked.contactToken.get)
                )
              )
          }
      )

      otherSidesAndIds <- EitherT.liftF(
        ConnectionsDAO.getOtherSideAndIdByConnectionTokens(
          connectionTokens,
          sender
        )
      )
      otherSidesAndIdsMap = otherSidesAndIds.map { case (token, id, participant) =>
        token -> (id -> participant)
      }.toMap

      messagesToInsert <- EitherT.fromEither[ConnectionIO](
        messages
          .map { message =>
            val token = message.connectionToken
            otherSidesAndIdsMap
              .get(token)
              .fold[Either[InsertMessagesError, CreateMessage]](
                Left(co(ConnectionNotFound(token)))
              ) { case (connectionId, recipientId) =>
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

      _ <- EitherT.liftF[doobie.ConnectionIO, InsertMessagesError, Unit](
        MessagesDAO.insert(messagesToInsert)
      )
    } yield messagesToInsert.map(_.id)

    query
      .transact(xa)
      .value
  }

  type AssertUserProvidedIdsNotExistError =
    MessagesAlreadyExist :+:
      MessageIdsNotUnique :+:
      CNil

  private def assertUserProvidedIdsNotExist(
      ids: List[MessageId]
  ): EitherT[ConnectionIO, AssertUserProvidedIdsNotExistError, Unit] = {
    val distinctIds = ids.distinct
    for {
      _ <-
        Either
          .cond(
            test = distinctIds.size == ids.size,
            right = (),
            left = co[AssertUserProvidedIdsNotExistError](
              MessageIdsNotUnique(ids.diff(distinctIds))
            )
          )
          .toEitherT[ConnectionIO]

      alreadyExistingMessages <- ids match {
        case head :: tail =>
          EitherT.liftF(
            MessagesDAO.getIdsOfAlreadyExistingMessages(
              NonEmptyList.of(head, tail: _*)
            )
          )
        case Nil =>
          EitherT.liftF(doobie.free.connection.pure(List.empty[MessageId]))
      }

      _ <-
        Either
          .cond(
            test = alreadyExistingMessages.isEmpty,
            right = (),
            left = co[AssertUserProvidedIdsNotExistError](
              MessagesAlreadyExist(alreadyExistingMessages)
            )
          )
          .toEitherT[ConnectionIO]
    } yield ()
  }

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): F[Either[GetMessagesPaginatedError, List[Message]]] = {
    implicit val loggingContext: LoggingContext = LoggingContext(
      "recipientId" -> recipientId,
      "limit" -> limit,
      "lastSeenMessageId" -> lastSeenMessageId
    )

    if (limit <= 0)
      InvalidLimitError(limit.toString).logWarnNew
        .asLeft[List[Message]]
        .leftMap[GetMessagesPaginatedError](co(_))
        .pure[F]
    else
      MessagesDAO
        .getMessagesPaginated(recipientId, limit, lastSeenMessageId)
        .logSQLErrors(
          s"getting messages paginated, recipient id - $recipientId",
          logger
        )
        .map(_.asRight[GetMessagesPaginatedError])
        .transact(xa)
  }

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): Stream[F, Message] =
    MessagesDAO.getMessageStream(recipientId, lastSeenMessageId).transact(xa)

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): F[List[Message]] =
    MessagesDAO
      .getConnectionMessages(recipientId, connectionId)
      .logSQLErrors(
        s"getting connection messages, connection id - $connectionId",
        logger
      )
      .transact(xa)
}
