package io.iohk.atala.prism.node.repositories.metrics

import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.{AtalaObjectId, AtalaObjectInfo, AtalaOperationInfo, AtalaOperationStatus}
import io.iohk.atala.prism.node.repositories.AtalaOperationsRepository
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import tofu.higherKind.Mid
import cats.effect.MonadCancelThrow

private[repositories] final class AtalaOperationsRepositoryMetrics[F[
    _
]: TimeMeasureMetric: MonadCancelThrow]
    extends AtalaOperationsRepository[Mid[F, *]] {
  private val repoName = "AtalaOperationsRepository"

  private lazy val insertObjectAndOperationsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "insertObjectAndOperations")

  private lazy val mergeObjectsTimerTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "mergeObjects")

  private lazy val getOperationInfoTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getOperationInfo")

  def insertOperation(
      objectId: AtalaObjectId,
      objectBytes: Array[Byte],
      atalaOperationIds: AtalaOperationId,
      atalaOperationsStatus: AtalaOperationStatus
  ): Mid[F, Either[NodeError, (Int, Int)]] =
    _.measureOperationTime(insertObjectAndOperationsTimer)

  def updateMergedObjects(
      atalaObject: AtalaObjectInfo,
      operations: List[SignedAtalaOperation],
      oldObjects: List[AtalaObjectInfo]
  ): Mid[F, Either[NodeError, Unit]] =
    _.measureOperationTime(mergeObjectsTimerTimer)

  def getOperationInfo(
      atalaOperationId: AtalaOperationId
  ): Mid[F, Either[NodeError, Option[AtalaOperationInfo]]] =
    _.measureOperationTime(getOperationInfoTimer)
}
