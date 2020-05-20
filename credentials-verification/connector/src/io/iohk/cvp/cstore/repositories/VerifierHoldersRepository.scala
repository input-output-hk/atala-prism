package io.iohk.cvp.cstore.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.iohk.connector.errors.ConnectorError
import io.iohk.cvp.cstore.models.{Verifier, VerifierHolder}
import io.iohk.cvp.cstore.repositories.daos.VerifierHoldersDAO
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

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
}
