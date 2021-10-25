package io.iohk.atala.prism.connector.repositories.daos

import cats.data.NonEmptyList
import doobie.implicits._
import cats.implicits._
import doobie.{FC, Fragments}
import doobie.implicits.legacy.instant._
import doobie.util.update.Update
import fs2.Stream
import io.iohk.atala.prism.connector.model.{ConnectionId, CreateMessage, Message, MessageId}
import io.iohk.atala.prism.models.ParticipantId

import java.time.Instant

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
         |VALUES ($id, $connection, $sender, $recipient, ${Instant
      .now()}, $content)""".stripMargin.update.run.void
  }

  def insert(
      messages: List[CreateMessage]
  ): doobie.ConnectionIO[Unit] = {
    Update[CreateMessage]("""
         |INSERT INTO messages (id, connection, sender, recipient, received_at, content)
         |VALUES (?, ?, ?, ?, ?, ?)""".stripMargin)
      .updateMany(messages)
      .flatTap { affectedRows =>
        FC.raiseError(
          new RuntimeException(
            s"Unknown error while inserting ${messages.size} messages, affected rows: $affectedRows"
          )
        ).whenA(messages.size != affectedRows)
      }
      .void
  }

  def getMessage(id: MessageId): doobie.ConnectionIO[Option[Message]] = {
    sql"""
         |SELECT id, connection, recipient, received_at, content
         |FROM messages
         |WHERE id = $id
       """.stripMargin.query[Message].option
  }

  def getIdsOfAlreadyExistingMessages(
      ids: NonEmptyList[MessageId]
  ): doobie.ConnectionIO[List[MessageId]] = {
    (fr"""
         |SELECT id
         |FROM messages
         |WHERE""".stripMargin ++ Fragments.in(fr"id", ids))
      .query[MessageId]
      .to[List]
  }

  def getMessagesPaginated(
      recipientId: ParticipantId,
      limit: Int,
      lastSeenMessageId: Option[MessageId]
  ): doobie.ConnectionIO[List[Message]] = {
    val query = lastSeenMessageId match {
      case Some(value) =>
        sql"""
             |WITH CTE AS (
             |  SELECT received_at AS last_seen_time
             |  FROM messages
             |  WHERE id = $value
             |)
             |SELECT id, connection, recipient, received_at, content
             |FROM CTE CROSS JOIN messages
             |WHERE recipient = $recipientId AND
             |      (received_at > last_seen_time OR (received_at = last_seen_time AND id > $value))
             |ORDER BY received_at ASC, id
             |LIMIT $limit
       """.stripMargin

      case None =>
        sql"""
             |SELECT id, connection, recipient, received_at, content
             |FROM messages
             |WHERE recipient = $recipientId
             |ORDER BY received_at ASC, id
             |LIMIT $limit
       """.stripMargin
    }

    query.query[Message].to[List]
  }

  def getConnectionMessages(
      recipientId: ParticipantId,
      connectionId: ConnectionId
  ): doobie.ConnectionIO[List[Message]] = {
    sql"""
         |SELECT id, connection, recipient, received_at, content
         |FROM messages
         |WHERE recipient = $recipientId AND
         |      connection = $connectionId
         |ORDER BY received_at ASC, id
       """.stripMargin.query[Message].to[List]
  }

  def deleteConnectionMessages(
      connectionId: ConnectionId
  ): doobie.ConnectionIO[Unit] = {
    sql"""
         |DELETE FROM messages
         |WHERE connection = $connectionId
       """.stripMargin.update.run.void
  }

  def getMessageStream(
      recipientId: ParticipantId,
      lastSeenMessageId: Option[MessageId]
  ): Stream[doobie.ConnectionIO, Message] = {
    val query = lastSeenMessageId match {
      case Some(value) =>
        sql"""
             |WITH CTE AS (
             |  SELECT received_at AS last_seen_time
             |  FROM messages
             |  WHERE id = $value
             |)
             |SELECT id, connection, recipient, received_at, content
             |FROM CTE CROSS JOIN messages
             |WHERE recipient = $recipientId AND
             |      (received_at > last_seen_time OR (received_at = last_seen_time AND id > $value))
             |ORDER BY received_at ASC, id
       """.stripMargin

      case None =>
        sql"""
             |SELECT id, connection, recipient, received_at, content
             |FROM messages
             |WHERE recipient = $recipientId
             |ORDER BY received_at ASC, id
       """.stripMargin
    }

    query.query[Message].stream
  }
}
