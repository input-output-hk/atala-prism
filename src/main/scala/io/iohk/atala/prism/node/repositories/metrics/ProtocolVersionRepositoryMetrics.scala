package io.iohk.atala.prism.node.repositories.metrics

import io.iohk.atala.prism.node.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.node.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.node.models.{ProtocolVersion, ProtocolVersionInfo}
import io.iohk.atala.prism.node.repositories.ProtocolVersionRepository
import tofu.higherKind.Mid
import cats.effect.MonadCancelThrow

private[repositories] final class ProtocolVersionRepositoryMetrics[F[
    _
]: TimeMeasureMetric: MonadCancelThrow]
    extends ProtocolVersionRepository[Mid[F, *]] {

  private val repoName = "ProtocolVersionRepository"
  private lazy val getCurrentProtocolVersionTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getCurrentProtocolVersion")
  private lazy val markEffectiveTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "markEffective")
  private lazy val isNodeSupportsOutdatedTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "isNodeSupportsOutdated")

  override def getCurrentProtocolVersion(): Mid[F, ProtocolVersion] =
    _.measureOperationTime(getCurrentProtocolVersionTimer)

  override def markEffective(
      blockLevel: Int
  ): Mid[F, Option[ProtocolVersionInfo]] =
    _.measureOperationTime(markEffectiveTimer)

  override def ifNodeSupportsCurrentProtocol(): Mid[F, Either[ProtocolVersion, Unit]] =
    _.measureOperationTime(isNodeSupportsOutdatedTimer)

}
