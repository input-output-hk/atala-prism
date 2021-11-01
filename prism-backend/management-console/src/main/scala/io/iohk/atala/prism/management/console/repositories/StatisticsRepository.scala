package io.iohk.atala.prism.management.console.repositories

import cats.{Comonad, Functor, Monad}
import cats.effect.Resource
import cats.syntax.comonad._
import cats.syntax.functor._
import derevo.tagless.applyK
import derevo.derive
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.models.{ParticipantId, Statistics, TimeInterval}
import io.iohk.atala.prism.management.console.repositories.daos.StatisticsDAO
import io.iohk.atala.prism.management.console.repositories.logs.StatisticsRepositoryLogs
import io.iohk.atala.prism.management.console.repositories.metrics.StatisticsRepositoryMetrics
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps
import cats.effect.MonadCancelThrow

@derive(applyK)
trait StatisticsRepository[F[_]] {
  def query(
      participantId: ParticipantId,
      timeIntervalMaybe: Option[TimeInterval]
  ): F[Statistics]
}

object StatisticsRepository {
  def apply[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[StatisticsRepository[F]] =
    for {
      serviceLogs <- logs.service[StatisticsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, StatisticsRepository[F]] =
        serviceLogs
      val metrics: StatisticsRepository[Mid[F, *]] =
        new StatisticsRepositoryMetrics[F]
      val logs: StatisticsRepository[Mid[F, *]] =
        new StatisticsRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new StatisticsRepositoryImpl[F](transactor)
    }

  def unsafe[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): StatisticsRepository[F] = StatisticsRepository(transactor, logs).extract

  def makeResource[F[_]: TimeMeasureMetric: MonadCancelThrow, R[_]: Monad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, StatisticsRepository[F]] =
    Resource.eval(StatisticsRepository(transactor, logs))
}

private final class StatisticsRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends StatisticsRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def query(
      participantId: ParticipantId,
      timeIntervalMaybe: Option[TimeInterval]
  ): F[Statistics] =
    StatisticsDAO
      .query(participantId, timeIntervalMaybe)
      .logSQLErrors(
        s"getting statistics, participant id - $participantId",
        logger
      )
      .transact(xa)
}
