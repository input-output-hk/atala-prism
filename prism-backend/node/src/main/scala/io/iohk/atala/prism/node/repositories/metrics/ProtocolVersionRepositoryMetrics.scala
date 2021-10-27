package io.iohk.atala.prism.node.repositories.metrics

import cats.effect.BracketThrow
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.node.models.{ProtocolVersion, ProtocolVersionInfo}
import io.iohk.atala.prism.node.repositories.ProtocolVersionRepository
import tofu.higherKind.Mid

private[repositories] final class ProtocolVersionRepositoryMetrics[F[
    _
]: TimeMeasureMetric: BracketThrow]
    extends ProtocolVersionRepository[Mid[F, *]] {

  private val repoName = "ProtocolVersionRepository"
  private lazy val markEffectiveTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "markEffective")
  private lazy val isNodeSupportsOutdatedTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "isNodeSupportsOutdated")

  override def markEffective(
      blockLevel: Int
  ): Mid[F, Option[ProtocolVersionInfo]] =
    _.measureOperationTime(markEffectiveTimer)

  override def ifNodeSupportsCurrentProtocol(): Mid[F, Either[ProtocolVersion, Unit]] =
    _.measureOperationTime(isNodeSupportsOutdatedTimer)
}
