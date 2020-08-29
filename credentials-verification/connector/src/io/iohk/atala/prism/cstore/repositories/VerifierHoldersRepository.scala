package io.iohk.atala.prism.cstore.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.iohk.atala.prism.cstore.repositories.daos.VerifierHoldersDAO
import io.iohk.connector.errors.ConnectorError
import io.iohk.atala.prism.cstore.models.{Verifier, VerifierHolder}
import io.iohk.atala.prism.cstore.repositories.daos.VerifierHoldersDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class VerifierHoldersRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(verifierId: Verifier.Id, json: Json): FutureEither[ConnectorError, VerifierHolder] = {
    VerifierHoldersDAO
      .insert(verifierId, json)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def list(
      verifierId: Verifier.Id,
      lastSeen: Option[Verifier.Id],
      limit: Int
  ): FutureEither[ConnectorError, Seq[VerifierHolder]] = {
    VerifierHoldersDAO
      .list(verifierId, lastSeen, limit)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
