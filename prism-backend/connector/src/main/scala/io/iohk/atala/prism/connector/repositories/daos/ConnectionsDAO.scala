package io.iohk.atala.prism.connector.repositories.daos

import java.time.Instant
import cats.data.NonEmptyList
import doobie.implicits._
import doobie.implicits.legacy.instant._
import doobie.util.fragments.{in, whereAnd}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.connector.model.{
  Connection,
  ConnectionId,
  ConnectionInfo,
  ConnectionStatus,
  ContactConnection,
  RawConnection,
  TokenString
}

object ConnectionsDAO {

  def insert(
      initiator: ParticipantId,
      acceptor: ParticipantId,
      token: TokenString,
      connectionStatus: ConnectionStatus
  ): doobie.ConnectionIO[(ConnectionId, Instant)] = {
    val connectionId = ConnectionId.random()

    sql"""
         |INSERT INTO connections (id, initiator, acceptor, token, instantiated_at, status)
         |VALUES ($connectionId, $initiator, $acceptor, $token, now(), $connectionStatus::CONTACT_CONNECTION_STATUS_TYPE)
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
    val connectionStatus: ConnectionStatus = ConnectionStatus.InvitationMissing

    sql"""
         |INSERT INTO connections (id, initiator, acceptor, token, instantiated_at, status)
         |VALUES ($connectionId, $initiator, $acceptor, $token, $instantiatedAt, $connectionStatus::CONTACT_CONNECTION_STATUS_TYPE)
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

  def getRawConnection(connectionId: ConnectionId): doobie.ConnectionIO[Option[RawConnection]] = {
    sql"""
         |SELECT id, initiator, acceptor, token, instantiated_at, status
         |FROM connections
         |WHERE id = $connectionId""".stripMargin
      .query[RawConnection]
      .option
  }

  def revoke(connectionId: ConnectionId): doobie.ConnectionIO[Int] = {
    val connectionStatus: ConnectionStatus = ConnectionStatus.ConnectionRevoked
    sql"""
         |UPDATE connections
         |SET status = $connectionStatus::CONTACT_CONNECTION_STATUS_TYPE
         |WHERE id = $connectionId
         """.stripMargin.update.run
  }

  def getOtherSide(connection: ConnectionId, participant: ParticipantId): doobie.ConnectionIO[Option[ParticipantId]] = {
    sql"""
         |SELECT acceptor AS other_side FROM connections WHERE id = $connection AND initiator = $participant
         | UNION
         | SELECT initiator AS other_side FROM connections WHERE id = $connection AND acceptor = $participant""".stripMargin
      .query[ParticipantId]
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
             |  SELECT id, acceptor AS side, instantiated_at, token, status FROM connections WHERE initiator = $participant
             | ),
             | accepted_connections AS (
             |  SELECT id, initiator as side, instantiated_at, token, status FROM connections WHERE acceptor = $participant
             | ),
             | all_connections AS (
             |  SELECT * FROM initiated_connections UNION SELECT * FROM accepted_connections
             | )
             |SELECT c.id, c.instantiated_at, p.id, p.tpe, p.public_key, p.name, p.did, p.logo, p.transaction_id, p.ledger, c.token, c.status
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
             |  SELECT id, acceptor AS side, instantiated_at, token, status FROM connections WHERE initiator = $participant),
             | accepted_connections AS (
             |  SELECT id, initiator as side, instantiated_at, token, status FROM connections WHERE acceptor = $participant),
             | all_connections AS (SELECT * FROM initiated_connections UNION SELECT * FROM accepted_connections)
             |SELECT c.id, c.instantiated_at, p.id, p.tpe, p.public_key, p.name, p.did, p.logo, p.transaction_id, p.ledger, c.token, c.status
             |FROM all_connections c
             |JOIN participants p ON p.id = c.side
             |ORDER BY c.instantiated_at ASC, c.id
             |LIMIT $limit
      """.stripMargin.query[ConnectionInfo].to[Seq]
    }
  }

  def getAcceptorConnections(acceptorIds: List[ParticipantId]): doobie.ConnectionIO[List[ContactConnection]] = {
    NonEmptyList.fromList(acceptorIds) match {
      case Some(acceptorIdsNonEmpty) =>
        val fragment =
          fr"SELECT acceptor, id, token, status FROM connections" ++
            whereAnd(in(fr"acceptor", acceptorIdsNonEmpty))
        fragment.query[(ParticipantId, ContactConnection)].toMap.map { contactConnectionMap =>
          acceptorIds.map(
            contactConnectionMap.getOrElse(
              _,
              ContactConnection(None, None, ConnectionStatus.InvitationMissing)
            )
          )
        }
      case None =>
        doobie.free.connection.pure(List.empty)
    }
  }
}
