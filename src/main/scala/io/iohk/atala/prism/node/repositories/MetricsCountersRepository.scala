package io.iohk.atala.prism.node.repositories

import cats.{Applicative, Comonad, Functor}
import cats.effect.{MonadCancelThrow, Resource}
import cats.implicits._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.node.metrics.TimeMeasureMetric
import io.iohk.atala.prism.node.repositories.daos.MetricsCountersDAO
import io.iohk.atala.prism.node.repositories.logs.MetricsCountersRepositoryLogs
import io.iohk.atala.prism.node.repositories.metrics.MetricsCountersRepositoryMetrics
import io.iohk.atala.prism.node.utils.syntax.DBConnectionOps
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait MetricsCountersRepository[F[_]] {

  /** Gets the counter by metric name
    */
  def getCounter(counterName: String): F[Int]
}

object MetricsCountersRepository {
  def apply[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[MetricsCountersRepository[F]] =
    for {
      serviceLogs <- logs.service[MetricsCountersRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, MetricsCountersRepository[F]] =
        serviceLogs
      val metrics: MetricsCountersRepository[Mid[F, *]] = new MetricsCountersRepositoryMetrics[F]()
      val logs: MetricsCountersRepository[Mid[F, *]] = new MetricsCountersRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new MetricsCountersRepositoryImpl[F](transactor)
    }

  def resource[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Applicative](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, MetricsCountersRepository[F]] =
    Resource.eval(MetricsCountersRepository(transactor, logs))

  def unsafe[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): MetricsCountersRepository[F] =
    MetricsCountersRepository(transactor, logs).extract
}

private final class MetricsCountersRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends MetricsCountersRepository[F] {
  def getCounter(counterName: String): F[Int] =
    MetricsCountersDAO
      .getCounter(counterName)
      .logSQLErrorsV2(f"getting counter $counterName")
      .transact(xa)
}
