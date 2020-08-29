package io.iohk.connector.repositories.daos

import doobie.implicits._
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.connector.model.{ConnectionId, Message, MessageId}

object MessagesDAO {
  def insert(
      id: MessageId,
      connection: ConnectionId,
      sender: ParticipantId,
      recipient: ParticipantId,
      content: Array[Byte]
  ): doobie.ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO messages (id, connection, sender, recipient, received_at, content)
         |VALUES ($id, $connection, $sender, $recipient, now(), $content)""".stripMargin.update.run.map(_ => ())
  }

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): doobie.ConnectionIO[Seq[Message]] = {
    val query = lastSeenMessageId match {
      case Some(value) =>
        sql"""
             |WITH CTE AS (
             |  SELECT received_at AS last_seen_time
             |  FROM messages
             |  WHERE id = $value
             |)
             |SELECT id, connection, received_at, content
             |FROM CTE CROSS JOIN messages
             |WHERE recipient = $recipientId AND
             |      (received_at > last_seen_time OR (received_at = last_seen_time AND id > $value))
             |ORDER BY received_at ASC, id
             |LIMIT $limit
       """.stripMargin

      case None =>
        sql"""
             |SELECT id, connection, received_at, content
             |FROM messages
             |WHERE recipient = $recipientId
             |ORDER BY received_at ASC, id
             |LIMIT $limit
       """.stripMargin
    }

    query.query[Message].to[Seq]
  }

  def getMessagesPaginated(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): doobie.ConnectionIO[Seq[Message]] = {
    sql"""
         |SELECT id, connection, received_at, content
         |FROM messages
         |WHERE recipient = $recipientId AND
         |      connection = $connectionId
         |ORDER BY received_at ASC, id
       """.stripMargin.query[Message].to[Seq]
  }
}
