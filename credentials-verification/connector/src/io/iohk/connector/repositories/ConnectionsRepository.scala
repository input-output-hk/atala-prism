package io.iohk.connector.repositories

import java.time.Instant

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model._
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.{FutureEitherOps, FutureOptionOps}

import scala.concurrent.ExecutionContext

class ConnectionsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def insertToken(initiator: ParticipantId, token: TokenString): FutureEither[Nothing, TokenString] = {
    val query =
      sql"""
            |INSERT INTO connection_tokens
            |  (token, initiator)
            |VALUES ($token, $initiator)
            """.stripMargin.update.run

    query
      .transact(xa)
      .unsafeToFuture()
      .map(_ => Right(token))
      .toFutureEither
  }

  // TODO: replace Unit left with real error support
  def getTokenInfo(token: TokenString): FutureEither[Unit, ParticipantInfo] = {
    val query =
      sql"""
         |SELECT t.token, p.id, p.tpe, p.name, p.did
         | FROM connection_tokens t
         | JOIN participants p ON p.id = t.initiator
         | WHERE t.token = $token
      """.stripMargin.query[(String, ParticipantInfo)].option

    query
      .map(_.map(_._2))
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(())
  }

  // TODO: replace Unit left with real error support
  def addConnectionFromToken(token: TokenString, acceptor: ParticipantId): FutureEither[Unit, ConnectionInfo] = {
    val now = Instant.now()

    def getTokenInitiatorQuery = {
      sql"""
           |SELECT initiator from connection_tokens WHERE token = $token AND used_at IS NULL
           """.stripMargin.query[ParticipantId].unique // TODO: use option instead, add error support
    }

    def getParticipantInfoQuery(participantId: ParticipantId) = {
      sql"""
           |SELECT id, tpe, name, did
           |FROM participants
           |WHERE id = $participantId""".stripMargin.query[ParticipantInfo].unique // TODO: use option instead, add error support
    }

    def createConnectionQuery(initiator: ParticipantId) = {
      sql"""
           |INSERT INTO connections (
           |  id, initiator, acceptor, instantiated_at
           |) VALUES (
           |  ${ConnectionId.random()}, $initiator, $acceptor, $now
           |) RETURNING id, instantiated_at""".stripMargin.query[(ConnectionId, Instant)].unique // TODO: use option instead, add error support
    }

    def deleteTokenQuery = {
      sql"""UPDATE connection_tokens SET used_at = now() WHERE token=$token""".update.run
    }

    val query = for {
      initiator <- getTokenInitiatorQuery
      initiatorInfo <- getParticipantInfoQuery(initiator)

      ciia <- createConnectionQuery(initiator)
      (connectionId, instantiatedAt) = ciia

      _ <- deleteTokenQuery
    } yield ConnectionInfo(connectionId, instantiatedAt, initiatorInfo)

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getConnectionsSince(
      participant: ParticipantId,
      since: Instant,
      limit: Int
  ): FutureEither[Nothing, Seq[ConnectionInfo]] = {
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

    val query = fullSql.query[ConnectionInfo].to[Seq]

    query
      .transact(xa)
      .unsafeToFuture()
      .map(seq => Right(seq))
      .toFutureEither
  }
}
