package io.iohk.atala.prism.management.console.repositories

import cats.{Comonad, Functor, Monad}
import cats.effect.Resource
import cats.syntax.comonad._
import cats.syntax.functor._
import derevo.tagless.applyK
import derevo.derive
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.daos.RequestNoncesDAO
import io.iohk.atala.prism.management.console.repositories.logs.RequestNoncesRepositoryLogs
import io.iohk.atala.prism.management.console.repositories.metrics.RequestNoncesRepositoryMetrics
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps
import cats.effect.MonadCancelThrow

@derive(applyK)
trait RequestNoncesRepository[F[_]] {
  def burn(participantId: ParticipantId, requestNonce: RequestNonce): F[Unit]
}

object RequestNoncesRepository {

  def apply[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[RequestNoncesRepository[F]] =
    for {
      serviceLogs <- logs.service[RequestNoncesRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, RequestNoncesRepository[F]] =
        serviceLogs
      val metrics: RequestNoncesRepository[Mid[F, *]] =
        new RequestNoncesRepositoryMetrics[F]
      val logs: RequestNoncesRepository[Mid[F, *]] =
        new RequestNoncesRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new RequestNoncesRepositoryPostgresImpl[F](transactor)
    }

  def unsafe[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): RequestNoncesRepository[F] =
    RequestNoncesRepository(transactor, logs).extract

  def makeResource[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Monad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, RequestNoncesRepository[F]] =
    Resource.eval(RequestNoncesRepository(transactor, logs))

}

private final class RequestNoncesRepositoryPostgresImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends RequestNoncesRepository[F] {
  override def burn(
      participantId: ParticipantId,
      requestNonce: RequestNonce
  ): F[Unit] =
    RequestNoncesDAO
      .burn(participantId, requestNonce)
      .logSQLErrorsV2(s"burning, participant id - $participantId")
      .transact(xa)
}
