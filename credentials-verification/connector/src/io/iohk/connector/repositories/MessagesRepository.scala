package io.iohk.connector.repositories

import java.time.Instant

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model._
import io.iohk.connector.repositories.daos.{ConnectionsDAO, MessagesDAO}
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class MessagesRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def insertMessage(
      sender: ParticipantId,
      connection: ConnectionId,
      content: Array[Byte]
  ): FutureEither[Nothing, MessageId] = {
    val messageId = MessageId.random()

    val query = for {
      recipient <- ConnectionsDAO.getOtherSide(connection, sender)
      _ <- MessagesDAO.insert(messageId, connection, sender, recipient, content)
    } yield messageId

    query
      .transact(xa)
      .unsafeToFuture()
      .map(_ => Right(messageId))
      .toFutureEither
  }

  def getMessagesSince(recipientId: ParticipantId, since: Instant, limit: Int): FutureEither[Nothing, Seq[Message]] = {
    MessagesDAO
      .getMessagesSince(recipientId, since, limit)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
