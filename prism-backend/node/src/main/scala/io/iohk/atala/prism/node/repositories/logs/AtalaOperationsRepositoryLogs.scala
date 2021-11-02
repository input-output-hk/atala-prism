package io.iohk.atala.prism.node.repositories.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.node.{errors, models}
import io.iohk.atala.prism.node.models.{AtalaObjectId, AtalaObjectInfo}
import io.iohk.atala.prism.node.repositories.AtalaOperationsRepository
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow
import io.iohk.atala.prism.node.errors.NodeError

private[repositories] final class AtalaOperationsRepositoryLogs[F[
    _
]: ServiceLogging[
  *[_],
  AtalaOperationsRepository[F]
]: MonadThrow]
    extends AtalaOperationsRepository[Mid[F, *]] {
  override def insertOperation(
      objectId: AtalaObjectId,
      objectBytes: Array[Byte],
      atalaOperationId: AtalaOperationId,
      atalaOperationStatus: models.AtalaOperationStatus
  ): Mid[F, Either[errors.NodeError, (Int, Int)]] =
    in =>
      info"inserting operation $objectId $atalaOperationId status - ${atalaOperationStatus.entryName}" *>
        in.flatTap(
          _.fold(
            err => error"Encountered an error while inserting operation $err",
            _ => info"inserting operation - successfully done"
          )
        ).onError(
          errorCause"Encountered an error while inserting operation" (_)
        )

  override def updateMergedObjects(
      atalaObject: AtalaObjectInfo,
      operations: List[SignedAtalaOperation],
      oldObjects: List[AtalaObjectInfo]
  ): Mid[F, Either[errors.NodeError, Unit]] =
    in =>
      info"updating merged objects ${atalaObject.objectId} operations - ${operations.size} old objects - ${oldObjects.size}" *>
        in.flatTap(
          _.fold(
            err => error"Encountered an error while updating merged objects $err",
            _ => info"updating merged objects - successfully done"
          )
        ).onError(
          errorCause"Encountered an error while updating merged objects" (_)
        )

  override def getOperationInfo(
      atalaOperationId: AtalaOperationId
  ): Mid[F, Either[NodeError, Option[models.AtalaOperationInfo]]] =
    in =>
      info"getting operation info $atalaOperationId" *>
        in.flatTap(
          _.fold(
            err => error"Encountered an error while getting operation info $err",
            maybeResult => info"getting operation info - got ${maybeResult.fold("nothing")(_.transactionId.toString)}"
          )
        ).onError(
          errorCause"Encountered an error while updating merged objects" (_)
        )
}
