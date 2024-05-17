package io.iohk.atala.prism.node.repositories.metrics

import cats.effect.MonadCancelThrow
import io.iohk.atala.prism.node.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.node.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.node.repositories.MetricsCountersRepository
import tofu.higherKind.Mid

private[repositories] final class MetricsCountersRepositoryMetrics[F[
    _
]: TimeMeasureMetric: MonadCancelThrow]
    extends MetricsCountersRepository[Mid[F, *]] {
  private val repoName = "MetricsCountersRepository"

  private lazy val getCounterTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getCounter")

  /** Gets the counter by metric name
    */
  override def getCounter(counterName: String): Mid[F, Int] = _.measureOperationTime(getCounterTimer)
}
