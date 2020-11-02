package io.iohk.atala.prism.vault.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.vault.repositories.daos.RequestNoncesDAO

import scala.concurrent.ExecutionContext

trait RequestNoncesRepository {
  def burn(did: String, requestNonce: RequestNonce): FutureEither[Nothing, Unit]
}

object RequestNoncesRepository {
  class PostgresImpl(xa: Transactor[IO])(implicit ec: ExecutionContext) extends RequestNoncesRepository {
    override def burn(did: String, requestNonce: RequestNonce): FutureEither[Nothing, Unit] = {
      RequestNoncesDAO
        .burn(did, requestNonce)
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }
  }
}
