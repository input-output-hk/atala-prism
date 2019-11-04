package io.iohk.cvp.cmanager.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.cmanager.models.Credential
import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.repositories.daos.CredentialsDAO
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class CredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {

  def create(data: CreateCredential): FutureEither[Nothing, Credential] = {
    CredentialsDAO
      .create(data)
      .transact(xa)
      .unsafeToFuture
      .map(Right(_))
      .toFutureEither
  }
}
