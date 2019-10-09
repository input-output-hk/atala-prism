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
    xa: Transactor[IO]
)(implicit ec: ExecutionContext) {

  def insertToken(initiator: ParticipantId, token: TokenString): FutureEither[Nothing, TokenString] = {
    ConnectionTokensDAO
      .insert(initiator, token)
      .transact(xa)
      .unsafeToFuture()
      .map(_ => Right(token))
      .toFutureEither
  }

  // TODO: replace Unit left with real error support
  def getTokenInfo(token: TokenString): FutureEither[Unit, ParticipantInfo] = {
    ParticipantsDAO
      .findBy(token)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither(())
  }

  // TODO: replace Unit left with real error support
  def addConnectionFromToken(token: TokenString, acceptor: ParticipantId): FutureEither[Unit, ConnectionInfo] = {
    val query = for {
      initiatorMaybe <- ParticipantsDAO.findByAvailableToken(token)
      initiator = initiatorMaybe.getOrElse(throw new RuntimeException("The token is not available"))

      ciia <- ConnectionsDAO.insert(initiator = initiator.id, acceptor = acceptor)
      (connectionId, instantiatedAt) = ciia

      _ <- ConnectionTokensDAO.markAsUsed(token)
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
    ConnectionsDAO
      .getConnectionsSince(participant, since, limit)
      .transact(xa)
      .unsafeToFuture()
      .map(seq => Right(seq))
      .toFutureEither
  }
}
