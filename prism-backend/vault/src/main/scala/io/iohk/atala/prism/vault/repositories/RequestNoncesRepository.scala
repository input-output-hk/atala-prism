package io.iohk.atala.prism.vault.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.vault.repositories.daos.RequestNoncesDAO
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

trait RequestNoncesRepository {
  def burn(did: DID, requestNonce: RequestNonce): FutureEither[Nothing, Unit]
}

object RequestNoncesRepository {
  class PostgresImpl(xa: Transactor[IO])(implicit ec: ExecutionContext) extends RequestNoncesRepository {

    val logger: Logger = LoggerFactory.getLogger(getClass)

    override def burn(did: DID, requestNonce: RequestNonce): FutureEither[Nothing, Unit] = {
      RequestNoncesDAO
        .burn(did, requestNonce)
        .logSQLErrors("burning", logger)
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }
  }
}
