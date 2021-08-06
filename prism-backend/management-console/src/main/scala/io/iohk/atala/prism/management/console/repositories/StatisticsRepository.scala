package io.iohk.atala.prism.management.console.repositories

import cats.effect.BracketThrow
import derevo.tagless.applyK
import derevo.derive
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.models.{ParticipantId, Statistics, TimeInterval}
import io.iohk.atala.prism.management.console.repositories.daos.StatisticsDAO
import io.iohk.atala.prism.management.console.repositories.metrics.StatisticsRepositoryMetrics
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

@derive(applyK)
trait StatisticsRepository[F[_]] {
  def query(participantId: ParticipantId, timeIntervalMaybe: Option[TimeInterval]): F[Statistics]
}

object StatisticsRepository {
  def apply[F[_]: TimeMeasureMetric: BracketThrow](transactor: Transactor[F]): StatisticsRepository[F] = {
    val metrics: StatisticsRepository[Mid[F, *]] = new StatisticsRepositoryMetrics[F]
    metrics attach new StatisticsRepositoryImpl[F](transactor)
  }
}

private final class StatisticsRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F]) extends StatisticsRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def query(
      participantId: ParticipantId,
      timeIntervalMaybe: Option[TimeInterval]
  ): F[Statistics] =
    StatisticsDAO
      .query(participantId, timeIntervalMaybe)
      .logSQLErrors(s"getting statistics, participant id - $participantId", logger)
      .transact(xa)
}
