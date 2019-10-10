package io.iohk.connector.repositories.daos

import java.time.Instant

import doobie.implicits._
import io.iohk.connector.model.{ConnectionId, ConnectionInfo, ParticipantId}

object ConnectionsDAO {
  def insert(
      initiator: ParticipantId,
      acceptor: ParticipantId
  ): doobie.ConnectionIO[(ConnectionId, Instant)] = {

    val connectionId = ConnectionId.random()
    sql"""
         |INSERT INTO connections (id, initiator, acceptor, instantiated_at)
         |VALUES ($connectionId, $initiator, $acceptor, now())
         |RETURNING id, instantiated_at""".stripMargin
      .query[(ConnectionId, Instant)]
      .unique
  }

  def getOtherSide(connection: ConnectionId, participant: ParticipantId): doobie.ConnectionIO[ParticipantId] = {
    sql"""
         |SELECT acceptor AS other_side FROM connections WHERE id = $connection AND initiator = $participant
         | UNION
         | SELECT initiator AS other_side FROM connections WHERE id = $connection AND acceptor = $participant""".stripMargin
      .query[ParticipantId]
      .unique // TODO: use option, support error
  }

  def getConnectionsSince(
      participant: ParticipantId,
      since: Instant,
      limit: Int
  ): doobie.ConnectionIO[Seq[ConnectionInfo]] = {
    val baseSql = sql"""
                       |WITH
                       | initiated_connections AS (
                       |  SELECT id, acceptor AS side, instantiated_at FROM connections WHERE initiator = $participant),
                       | accepted_connections AS (
                       |  SELECT id, initiator as side, instantiated_at FROM connections WHERE acceptor = $participant),
                       | all_connections AS (SELECT * FROM initiated_connections UNION SELECT * FROM accepted_connections)
                       |SELECT c.id, c.instantiated_at, p.id, p.tpe, p.name, p.did
                       |FROM all_connections c
                       |JOIN participants p ON p.id = c.side
                       |WHERE c.instantiated_at >= $since
                       |ORDER BY c.instantiated_at ASC
      """.stripMargin

    val fullSql = if (limit == 0) baseSql else (baseSql ++ sql"LIMIT $limit")
    fullSql.query[ConnectionInfo].to[Seq]
  }
}
