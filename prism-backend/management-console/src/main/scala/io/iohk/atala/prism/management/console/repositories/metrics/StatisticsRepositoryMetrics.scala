package io.iohk.atala.prism.management.console.repositories.metrics

import io.iohk.atala.prism.management.console.models.{ParticipantId, Statistics, TimeInterval}
import io.iohk.atala.prism.management.console.repositories.StatisticsRepository
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import tofu.higherKind.Mid
import cats.effect.MonadCancelThrow

final class StatisticsRepositoryMetrics[F[_]: TimeMeasureMetric: MonadCancelThrow] extends StatisticsRepository[Mid[F, *]] {
  private val repoName = "StatisticsRepository"
  private lazy val queryTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "query")
  override def query(
      participantId: ParticipantId,
      timeIntervalMaybe: Option[TimeInterval]
  ): Mid[F, Statistics] =
    _.measureOperationTime(queryTimer)
}
