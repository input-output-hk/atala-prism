package io.iohk.atala.prism.connector.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.connector.model.RequestNonce
import io.iohk.atala.prism.connector.repositories.daos.RequestNoncesDAO

import scala.concurrent.ExecutionContext

trait RequestNoncesRepository {
  def burn(participantId: ParticipantId, requestNonce: RequestNonce): FutureEither[Nothing, Unit]
}

object RequestNoncesRepository {
  class PostgresImpl(xa: Transactor[IO])(implicit ec: ExecutionContext) extends RequestNoncesRepository {
    override def burn(participantId: ParticipantId, requestNonce: RequestNonce): FutureEither[Nothing, Unit] = {
      RequestNoncesDAO
        .burn(participantId, requestNonce)
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }
  }
}
