package io.iohk.atala.prism.connector.repositories

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import cats.effect.IO
import doobie.implicits._
import fs2.Stream
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors.{
  ConnectionNotFound,
  ConnectorError,
  ConnectorErrorSupport,
  InvalidArgumentError
}
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos.{ConnectionsDAO, MessagesDAO}
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.connector_models.MessageToSendByConnectionToken
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import org.slf4j.{Logger, LoggerFactory}

import java.time.Instant
import scala.concurrent.ExecutionContext

class MessagesRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) extends ConnectorErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte]
  ): FutureEither[Nothing, MessageId] = {
    val messageId = MessageId.random()

    val query = for {
      recipientOption <- ConnectionsDAO.getOtherSide(connection, sender)
      recipient = recipientOption.getOrElse(
        throw new RuntimeException(
          s"Failed to send message, the connection $connection with sender $sender doesn't exist"
        )
      )
      _ <- MessagesDAO.insert(messageId, connection, sender, recipient, content)
    } yield messageId

    query
      .transact(xa)
      .unsafeToFuture()
      .map(_ => Right(messageId))
      .toFutureEither
  }

  def insertMessages(
      sender: ParticipantId,
      messages: NonEmptyList[MessageToSendByConnectionToken]
  ): FutureEither[ConnectorError, List[MessageId]] = {
    val connectionTokens = messages.map(_.connectionToken).map(TokenString(_))

    val query = for {
      otherSidesAndIds <- EitherT.liftF(ConnectionsDAO.getOtherSideAndIdByConnectionTokens(connectionTokens, sender))
      otherSidesAndIdsMap = otherSidesAndIds.map { case (token, id, participant) => token -> (id -> participant) }.toMap

      messagesToInsert <- EitherT.fromEither[doobie.ConnectionIO](
        messages
          .map { message =>
            val token = TokenString(message.connectionToken)
            otherSidesAndIdsMap
              .get(token)
              .fold[Either[ConnectorError, CreateMessage]](Left(ConnectionNotFound(token))) {
                case (connectionId, recipientId) =>
                  Right(
                    CreateMessage(
                      id = MessageId.random(),
                      connection = connectionId,
                      sender = sender,
                      recipient = recipientId,
                      receivedAt = Instant.now(),
                      content = message.message.fold(Array.empty[Byte])(_.toByteArray)
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
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
