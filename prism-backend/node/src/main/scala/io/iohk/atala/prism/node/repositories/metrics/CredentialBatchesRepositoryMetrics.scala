package io.iohk.atala.prism.node.repositories.metrics

import cats.effect.BracketThrow
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, LedgerData}
import io.iohk.atala.prism.node.repositories.CredentialBatchesRepository
import tofu.higherKind.Mid

private[repositories] final class CredentialBatchesRepositoryMetrics[F[
    _
]: TimeMeasureMetric: BracketThrow]
    extends CredentialBatchesRepository[Mid[F, *]] {

  private val repoName = "CredentialBatchesRepository"
  private lazy val getBatchStateTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getBatchState")
  private lazy val getCredentialRevocationTimeTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getCredentialRevocationTime")

  override def getBatchState(
      batchId: CredentialBatchId
  ): Mid[F, Either[NodeError, Option[CredentialBatchState]]] =
    _.measureOperationTime(getBatchStateTimer)
  override def getCredentialRevocationTime(
      batchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): Mid[F, Either[NodeError, Option[LedgerData]]] =
    _.measureOperationTime(getCredentialRevocationTimeTimer)
}
