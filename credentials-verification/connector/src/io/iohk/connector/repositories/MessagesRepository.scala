package io.iohk.connector.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.errors.{ConnectorError, ErrorSupport, InvalidArgumentError, LoggingContext}
import io.iohk.connector.model._
import io.iohk.connector.repositories.daos.{ConnectionsDAO, MessagesDAO}
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class MessagesRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) extends ErrorSupport {

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

  def getMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): FutureEither[ConnectorError, Seq[Message]] = {
    MessagesDAO
      .getMessagesPaginated(recipientId, connectionId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
