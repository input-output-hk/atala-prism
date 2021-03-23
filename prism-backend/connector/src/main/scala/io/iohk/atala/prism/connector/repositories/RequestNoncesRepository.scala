package io.iohk.atala.prism.connector.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.connector.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.identity.DID
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

trait RequestNoncesRepository {
  def burn(participantId: ParticipantId, requestNonce: RequestNonce): FutureEither[Nothing, Unit]
  def burn(did: DID, requestNonce: RequestNonce): FutureEither[Nothing, Unit]
}

object RequestNoncesRepository {
  class PostgresImpl(xa: Transactor[IO])(implicit ec: ExecutionContext) extends RequestNoncesRepository {

    val logger: Logger = LoggerFactory.getLogger(getClass)

    override def burn(participantId: ParticipantId, requestNonce: RequestNonce): FutureEither[Nothing, Unit] = {
      RequestNoncesDAO
        .burn(participantId, requestNonce)
        .logSQLErrors(s"burning, participant id - $participantId", logger)
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }

    override def burn(did: DID, requestNonce: RequestNonce): FutureEither[Nothing, Unit] = {
      RequestNoncesDAO
        .burn(did, requestNonce)
        .logSQLErrors(s"burning, did - $did", logger)
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }
  }
}
