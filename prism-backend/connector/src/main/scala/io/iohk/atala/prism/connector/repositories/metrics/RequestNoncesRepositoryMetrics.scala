package io.iohk.atala.prism.connector.repositories.metrics

import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.repositories.RequestNoncesRepository
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid
import cats.effect.MonadCancel

private[repositories] final class RequestNoncesRepositoryMetrics[F[
    _
]: TimeMeasureMetric](implicit
    ce: MonadCancel[F, Throwable]
) extends RequestNoncesRepository[Mid[F, *]] {
  private val repoName = "ParticipantsRepository"
  private lazy val burnById =
    TimeMeasureUtil.createDBQueryTimer(repoName, "burnById")
  private lazy val burnByDid =
    TimeMeasureUtil.createDBQueryTimer(repoName, "burnByDid")

  override def burn(
      participantId: ParticipantId,
      requestNonce: RequestNonce
  ): Mid[F, Unit] =
    _.measureOperationTime(burnById)

  override def burn(did: DID, requestNonce: RequestNonce): Mid[F, Unit] =
    _.measureOperationTime(burnByDid)
}
