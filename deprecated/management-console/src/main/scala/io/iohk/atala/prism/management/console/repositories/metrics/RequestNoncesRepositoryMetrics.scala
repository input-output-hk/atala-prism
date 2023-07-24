package io.iohk.atala.prism.management.console.repositories.metrics

import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.RequestNoncesRepository
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import tofu.higherKind.Mid
import cats.effect.MonadCancelThrow

final class RequestNoncesRepositoryMetrics[F[
    _
]: TimeMeasureMetric: MonadCancelThrow]
    extends RequestNoncesRepository[Mid[F, *]] {
  private val repoName = "RequestNoncesRepositoryPostgresImpl"
  private lazy val burnTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "burn")
  override def burn(
      participantId: ParticipantId,
      requestNonce: RequestNonce
  ): Mid[F, Unit] =
    _.measureOperationTime(burnTimer)
}
