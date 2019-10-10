package io.iohk.connector.repositories

import java.time.Instant

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model._
import io.iohk.connector.repositories.daos.{ConnectionTokensDAO, ConnectionsDAO, ParticipantsDAO}
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.{FutureEitherOps, FutureOptionOps}

import scala.concurrent.ExecutionContext

class ConnectionsRepository(
    connectionTokensDAO: ConnectionTokensDAO,
    connectionsDAO: ConnectionsDAO,
    participantsDAO: ParticipantsDAO,
    xa: Transactor[IO]
)(implicit ec: ExecutionContext) {

  def insertToken(initiator: ParticipantId, token: TokenString): FutureEither[Nothing, TokenString] = {
    connectionTokensDAO
      .insert(initiator, token)
      .transact(xa)
      .unsafeToFuture()
      .map(_ => Right(token))
      .toFutureEither
  }

  // TODO: replace Unit left with real error support
  def getTokenInfo(token: TokenString): FutureEither[Unit, ParticipantInfo] = {
    participantsDAO
      .findBy(token)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(())
  }

  // TODO: replace Unit left with real error support
  def addConnectionFromToken(token: TokenString, acceptor: ParticipantId): FutureEither[Unit, ConnectionInfo] = {
    val now = Instant.now()

    val query = for {
      initiatorMaybe <- participantsDAO.findByAvailableToken(token)
      initiator = initiatorMaybe.getOrElse(throw new RuntimeException("The token is not available"))

      ciia <- connectionsDAO.insert(initiator = initiator.id, acceptor = acceptor, now)
      (connectionId, instantiatedAt) = ciia

      _ <- connectionTokensDAO.markAsUsed(token)
    } yield ConnectionInfo(connectionId, instantiatedAt, initiator)

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
    connectionsDAO
      .getConnectionsSince(participant, since, limit)
      .transact(xa)
      .unsafeToFuture()
      .map(seq => Right(seq))
      .toFutureEither
  }
}
