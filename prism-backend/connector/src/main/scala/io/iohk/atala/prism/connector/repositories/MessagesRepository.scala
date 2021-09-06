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
import io.iohk.atala.prism.connector.repositories.MessagesRepository.InsertMessageError._
import io.iohk.atala.prism.connector.repositories.MessagesRepository.InsertMessagesError._
import io.iohk.atala.prism.connector.repositories.MessagesRepository._
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

  def getMessageStream(recipientId: ParticipantId, lastSeenMessageId: Option[MessageId]): S[Message]

  def getConnectionMessages(recipientId: ParticipantId, connectionId: ConnectionId): F[List[Message]]
}

object MessagesRepository {
  // Repository public methods errors
  // A trait per method
  sealed trait InsertMessageError extends MessagesError
  object InsertMessageError {
    final case class MessagesAlreadyExistErrorIME(override val ids: List[MessageId])
      extends MessagesAlreadyExist(ids)
        with InsertMessageError

    final case class MessageIdsNotUniqueErrorIME(override val ids: List[MessageId])
      extends MessageIdsNotUnique(ids)
        with InsertMessageError

    final case class ConnectionNotFoundErrorIME(connectionId: ConnectionId)
      extends ConnectionNotFound(Right(connectionId))
        with InsertMessageError

    final case class ConnectionRevokedErrorIME(connectionId: ConnectionId)
      extends ConnectionRevoked(Right(connectionId))
        with InsertMessageError

    final case class ConnectionNotFoundByConnectionIdAndSenderErrorIME(override val sender: ParticipantId,
                                                                       override val connection: ConnectionId)
      extends ConnectionNotFoundByConnectionIdAndSender(sender, connection)
        with InsertMessageError

    def fromAssertUserProvidedIdsNotExistError(err: AssertUserProvidedIdsNotExistError): InsertMessageError = err match {
      case MessagesAlreadyExistError(ids) => MessagesAlreadyExistErrorIME(ids)
      case MessageIdsNotUniqueError(ids) => MessageIdsNotUniqueErrorIME(ids)
    }
  }

  sealed trait InsertMessagesError extends MessagesError
  object InsertMessagesError {
    final case class MessagesAlreadyExistErrorIMEs(override val ids: List[MessageId])
      extends MessagesAlreadyExist(ids)
        with InsertMessagesError

    final case class MessageIdsNotUniqueErrorIMEs(override val ids: List[MessageId])
      extends MessageIdsNotUnique(ids)
       with InsertMessagesError

    final case class ConnectionNotFoundErrorIMEs(token: TokenString)
      extends ConnectionNotFound(Left(token))
        with InsertMessagesError

    final case class ConnectionRevokedErrorIMEs(token: TokenString)
      extends ConnectionRevoked(Left(token))
        with InsertMessagesError

    def fromAssertUserProvidedIdsNotExistError(err: AssertUserProvidedIdsNotExistError): InsertMessagesError = err match {
      case MessagesAlreadyExistError(ids) => MessagesAlreadyExistErrorIMEs(ids)
      case MessageIdsNotUniqueError(ids) => MessageIdsNotUniqueErrorIMEs(ids)
    }
  }

  sealed trait GetMessagesPaginatedError extends MessagesError
  final case class InvalidLimitError(override val value: String)
    extends InvalidArgumentError("limit", "positive value", value)
      with GetMessagesPaginatedError

  // Auxiliary methods errors
  sealed trait AssertUserProvidedIdsNotExistError extends MessagesError
  final case class MessagesAlreadyExistError(override val ids: List[MessageId])
    extends MessagesAlreadyExist(ids)
      with AssertUserProvidedIdsNotExistError

  final case class MessageIdsNotUniqueError(override val ids: List[MessageId])
    extends MessageIdsNotUnique(ids)
      with AssertUserProvidedIdsNotExistError

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

  def insertMessage(
      sender: ParticipantId,
      connectionId: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId] = None
  ): F[Either[InsertMessageError, MessageId]] = {
    val messageId = messageIdOption.getOrElse(MessageId.random())

    val query: EitherT[ConnectionIO, InsertMessageError, MessageId] = for {
      _ <- assertUserProvidedIdsNotExist(messageIdOption.toList).leftMap(InsertMessageError.fromAssertUserProvidedIdsNotExistError)

      rawConnection <- EitherT[ConnectionIO, InsertMessageError, RawConnection](
        ConnectionsDAO.getRawConnection(connectionId)
        .map(_.toRight(ConnectionNotFoundErrorIME(connectionId))))

      _ <- EitherT.fromEither[ConnectionIO] {
        Either.cond[InsertMessageError, Unit](
          rawConnection.status != ConnectionStatus.ConnectionRevoked,
          right = (),
          left = ConnectionRevokedErrorIME(connectionId))
      }

      recipientOption <- EitherT.liftF(ConnectionsDAO.getOtherSide(connectionId, sender))

      recipient <-
        recipientOption
          .toRight(ConnectionNotFoundByConnectionIdAndSenderErrorIME(sender, connectionId))
          .toEitherT[ConnectionIO]

      _ <- EitherT.liftF[ConnectionIO, InsertMessageError, Unit](
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
      _ <- assertUserProvidedIdsNotExist(messages.toList.flatMap(_.id)).leftMap(InsertMessagesError.fromAssertUserProvidedIdsNotExistError)

      _ <- EitherT(ConnectionsDAO.getConnectionsByConnectionTokens(connectionTokens.toList)
        .map { connections =>
          connections.find(_.connectionStatus == ConnectionStatus.ConnectionRevoked)
            // We can be sure that a found revoked connection has a corresponding token, so .get is safe
            .toLeft(()).leftMap(revoked => ConnectionRevokedErrorIMEs(revoked.contactToken.get))
        })

      otherSidesAndIds <- EitherT.liftF(ConnectionsDAO.getOtherSideAndIdByConnectionTokens(connectionTokens, sender))
      otherSidesAndIdsMap = otherSidesAndIds.map { case (token, id, participant) => token -> (id -> participant) }.toMap

      messagesToInsert <- EitherT.fromEither[ConnectionIO](
        messages
          .map { message =>
            val token = message.connectionToken
            otherSidesAndIdsMap
              .get(token)
              .fold[Either[InsertMessagesError, CreateMessage]](Left(ConnectionNotFoundErrorIMEs(token))) {
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

      _ <- EitherT.liftF[doobie.ConnectionIO, InsertMessagesError, Unit](MessagesDAO.insert(messagesToInsert))
    } yield messagesToInsert.map(_.id)

    query
      .transact(xa)
      .value
  }

  private def assertUserProvidedIdsNotExist(ids: List[MessageId]): EitherT[ConnectionIO, AssertUserProvidedIdsNotExistError, Unit] = {
    val distinctIds = ids.distinct
    for {
      _ <-
        Either
          .cond(
            test = distinctIds.size == ids.size,
            right = (),
            left = MessageIdsNotUniqueError(ids.diff(distinctIds))
          )
          .toEitherT[ConnectionIO]

      alreadyExistingMessages <- ids match {
        case head :: tail => EitherT.liftF(MessagesDAO.getIdsOfAlreadyExistingMessages(NonEmptyList.of(head, tail: _*)))
        case Nil => EitherT.liftF(doobie.free.connection.pure(List.empty[MessageId]))
      }

      _ <-
        Either
          .cond[AssertUserProvidedIdsNotExistError, Unit](
            test = alreadyExistingMessages.isEmpty,
            right = (),
            left = MessagesAlreadyExistError(alreadyExistingMessages)
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
      InvalidLimitError(limit.toString)
        .logWarnNew
        .asLeft[List[Message]]
        .leftMap[GetMessagesPaginatedError](x => x)
        .pure[F]
    else
      MessagesDAO
        .getMessagesPaginated(recipientId, limit, lastSeenMessageId)
        .logSQLErrors(s"getting messages paginated, recipient id - $recipientId", logger)
        .map(_.asRight[GetMessagesPaginatedError])
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
