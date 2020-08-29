package io.iohk.atala.prism.cstore.services

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.errors.ConnectorError
import io.iohk.connector.model.TokenString
import io.iohk.connector.repositories.daos.ConnectionTokensDAO
import io.iohk.atala.prism.cstore.models.StoreIndividual
import io.iohk.atala.prism.cstore.repositories.daos.VerifierHoldersDAO
import io.iohk.atala.prism.cstore.repositories.daos.VerifierHoldersDAO.VerifierHolderCreateData
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class VerifierHoldersService(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def createIndividual(
      userId: ParticipantId,
      data: VerifierHolderCreateData
  ): FutureEither[ConnectorError, StoreIndividual] = {
    VerifierHoldersDAO
      .insertIndividual(userId, data)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getIndividuals(
      userId: ParticipantId,
      lastSeen: Option[ParticipantId],
      limit: Int
  ): FutureEither[ConnectorError, Seq[StoreIndividual]] = {
    VerifierHoldersDAO
      .listIndividuals(userId, lastSeen, limit)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def generateTokenFor(
      verifierId: ParticipantId,
      holderId: ParticipantId
  ): FutureEither[ConnectorError, TokenString] = {
    val token = TokenString.random()
    val query = for {
      _ <- ConnectionTokensDAO.insert(verifierId, token)
      _ <- VerifierHoldersDAO.setConnectionToken(verifierId, holderId, token)
    } yield token

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
