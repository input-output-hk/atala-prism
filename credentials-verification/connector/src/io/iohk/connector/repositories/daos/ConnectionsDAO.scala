package io.iohk.connector.repositories.daos

import java.time.Instant

import doobie.implicits._
import io.iohk.connector.model.{Connection, ConnectionId, ConnectionInfo, TokenString}
import io.iohk.cvp.models.ParticipantId

object ConnectionsDAO {
  def insert(
      initiator: ParticipantId,
      acceptor: ParticipantId,
      token: TokenString
  ): doobie.ConnectionIO[(ConnectionId, Instant)] = {

    val connectionId = ConnectionId.random()
    sql"""
         |INSERT INTO connections (id, initiator, acceptor, token, instantiated_at)
         |VALUES ($connectionId, $initiator, $acceptor, $token, now())
         |RETURNING id, instantiated_at""".stripMargin
      .query[(ConnectionId, Instant)]
      .unique
  }

  def insert(
      initiator: ParticipantId,
      acceptor: ParticipantId,
      instantiatedAt: Instant,
      token: TokenString
  ): doobie.ConnectionIO[ConnectionId] = {

    val connectionId = ConnectionId.random()
    sql"""
         |INSERT INTO connections (id, initiator, acceptor, token, instantiated_at)
         |VALUES ($connectionId, $initiator, $acceptor, $token, $instantiatedAt)
         |RETURNING id""".stripMargin
      .query[ConnectionId]
      .unique
  }

  def exists(connectionId: ConnectionId): doobie.ConnectionIO[Boolean] = {
    sql"""
         |SELECT 1 FROM connections WHERE id = $connectionId
       """.stripMargin
      .query[Int]
      .option
      .map(_.isDefined)
  }

  def getOtherSide(connection: ConnectionId, participant: ParticipantId): doobie.ConnectionIO[Option[ParticipantId]] = {
    sql"""
         |SELECT acceptor AS other_side FROM connections WHERE id = $connection AND initiator = $participant
         | UNION
         | SELECT initiator AS other_side FROM connections WHERE id = $connection AND acceptor = $participant""".stripMargin
      .query[ParticipantId]
      .option
  }

  def getConnection(id: ConnectionId): doobie.ConnectionIO[Option[Connection]] = {
    sql"""
         |SELECT token, id
         |FROM connections
         |WHERE id = $id
         |""".stripMargin
      .query[Connection]
      .option
  }

  def getConnectionByToken(token: TokenString): doobie.ConnectionIO[Option[Connection]] = {
    sql"""
         |SELECT token, id
         |FROM connections
         |WHERE token = ${token.token}
         |""".stripMargin
      .query[Connection]
      .option
  }

  def getConnectionsPaginated(
      participant: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): doobie.ConnectionIO[Seq[ConnectionInfo]] = {
    lastSeenConnectionId match {
      case Some(value) =>
        sql"""
             |WITH
             | CTE AS (
             |   SELECT instantiated_at AS last_seen_time
             |   FROM connections
             |   WHERE id = $value
             | ),
             | initiated_connections AS (
             |  SELECT id, acceptor AS side, instantiated_at, token FROM connections WHERE initiator = $participant
             | ),
             | accepted_connections AS (
             |  SELECT id, initiator as side, instantiated_at, token FROM connections WHERE acceptor = $participant
             | ),
             | all_connections AS (
             |  SELECT * FROM initiated_connections UNION SELECT * FROM accepted_connections
             | )
             |SELECT c.id, c.instantiated_at, p.id, p.tpe, p.public_key, p.name, p.did, p.logo, c.token
             |FROM CTE CROSS JOIN all_connections c
             |JOIN participants p ON p.id = c.side
             |WHERE c.instantiated_at > last_seen_time OR (instantiated_at = last_seen_time AND c.id > $value)
             |ORDER BY c.instantiated_at ASC, c.id
             |LIMIT $limit
      """.stripMargin.query[ConnectionInfo].to[Seq]
      case None =>
        sql"""
             |WITH
             | initiated_connections AS (
             |  SELECT id, acceptor AS side, instantiated_at, token FROM connections WHERE initiator = $participant),
             | accepted_connections AS (
             |  SELECT id, initiator as side, instantiated_at, token FROM connections WHERE acceptor = $participant),
             | all_connections AS (SELECT * FROM initiated_connections UNION SELECT * FROM accepted_connections)
             |SELECT c.id, c.instantiated_at, p.id, p.tpe, p.public_key, p.name, p.did, p.logo, c.token
             |FROM all_connections c
             |JOIN participants p ON p.id = c.side
             |ORDER BY c.instantiated_at ASC, c.id
             |LIMIT $limit
      """.stripMargin.query[ConnectionInfo].to[Seq]
    }
  }
}
