package io.iohk.atala.prism.connector.repositories

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import cats.effect.IO
import doobie.ConnectionIO
import doobie.implicits._
import fs2.Stream
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors.{
  ConnectionNotFound,
  ConnectionNotFoundByConnectionIdAndSender,
  ConnectorError,
  ConnectorErrorSupport,
  InvalidArgumentError,
  MessageIdsNotUnique,
  MessagesAlreadyExist
}
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.model.actions.SendMessagesRequest
import io.iohk.atala.prism.connector.repositories.daos.{ConnectionsDAO, MessagesDAO}
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

import java.time.Instant
import scala.concurrent.ExecutionContext

class MessagesRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) extends ConnectorErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte],
      messageIdOption: Option[MessageId] = None
  ): FutureEither[ConnectorError, MessageId] = {
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
      .unsafeToFuture()
      .toFutureEither
  }

  def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[SendMessagesRequest.MessageToSend]
  ): FutureEither[ConnectorError, List[MessageId]] = {
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
      .unsafeToFuture()
      .toFutureEither
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
  ): FutureEither[ConnectorError, Seq[Message]] = {
    implicit val loggingContext: LoggingContext = LoggingContext(
      "recipientId" -> recipientId,
      "limit" -> limit,
      "lastSeenMessageId" -> lastSeenMessageId
    )

    if (limit <= 0) {
      Left(InvalidArgumentError("limit", "positive value", limit.toString).logWarn).toFutureEither
    } else {
      MessagesDAO
        .getMessagesPaginated(recipientId, limit, lastSeenMessageId)
        .logSQLErrors(s"getting messages paginated, recipient id - $recipientId", logger)
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }
  }

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): Stream[IO, Message] = {
    MessagesDAO.getMessageStream(recipientId, lastSeenMessageId).transact(xa)
  }

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): FutureEither[ConnectorError, Seq[Message]] = {
    MessagesDAO
      .getConnectionMessages(recipientId, connectionId)
      .logSQLErrors(s"getting connection messages, connection id - $connectionId", logger)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
