package io.iohk.cvp.cstore.services

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.errors.ConnectorError
import io.iohk.connector.model.TokenString
import io.iohk.connector.repositories.daos.ConnectionTokensDAO
import io.iohk.cvp.cstore.models.StoreIndividual
import io.iohk.cvp.cstore.repositories.daos.IndividualsDAO
import io.iohk.cvp.cstore.repositories.daos.IndividualsDAO.StoreIndividualCreateData
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class StoreIndividualsService(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def createIndividual(
      userId: ParticipantId,
      data: StoreIndividualCreateData
  ): FutureEither[ConnectorError, StoreIndividual] = {
    IndividualsDAO
      .insert(userId, data)
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
    IndividualsDAO
      .list(userId, lastSeen, limit)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def generateTokenFor(
      userId: ParticipantId,
      individualId: ParticipantId
  ): FutureEither[ConnectorError, TokenString] = {
    val token = TokenString.random()
    val query = for {
      _ <- ConnectionTokensDAO.insert(userId, token)
      _ <- IndividualsDAO.setConnectionToken(userId, individualId, token)
    } yield token

    query
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
