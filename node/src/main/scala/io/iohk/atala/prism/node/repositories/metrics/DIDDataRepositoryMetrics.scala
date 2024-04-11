package io.iohk.atala.prism.node.repositories.metrics

import io.iohk.atala.prism.identity.CanonicalPrismDid
import io.iohk.atala.prism.node.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.node.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.repositories.DIDDataRepository
import tofu.higherKind.Mid
import cats.effect.MonadCancelThrow

private[repositories] final class DIDDataRepositoryMetrics[F[
    _
]: TimeMeasureMetric: MonadCancelThrow]
    extends DIDDataRepository[Mid[F, *]] {
  private lazy val findByDidTimer =
    TimeMeasureUtil.createDBQueryTimer("DIDDataRepository", "findByDid")
  override def findByDid(
      did: CanonicalPrismDid
  ): Mid[F, Either[NodeError, Option[DIDDataState]]] =
    _.measureOperationTime(findByDidTimer)
}
