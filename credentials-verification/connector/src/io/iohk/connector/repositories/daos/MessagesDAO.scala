package io.iohk.connector.repositories.daos

import java.time.Instant

import doobie.implicits._
import io.iohk.connector.model.{ConnectionId, Message, MessageId, ParticipantId, ParticipantInfo, TokenString}

object MessagesDAO {
  def insert(id: MessageId, connection: ConnectionId, sender: ParticipantId, recipient: ParticipantId, content: Array[Byte]): doobie.ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO messages (id, connection, sender, recipient, received_at, content)
         |VALUES ($id, $connection, $sender, $recipient, now(), $content)"""
      .stripMargin.update.run.map(_ => ())
  }

  def getMessagesSince(recipientId: ParticipantId, since: Instant, limit: Int): doobie.ConnectionIO[Seq[Message]] = {
    sql"""
         |SELECT id, connection, received_at, content
         |FROM messages
         |WHERE recipient = $recipientId AND received_at >= $since
         |LIMIT $limit
       """.stripMargin.query[Message].to[Seq]
  }
}
