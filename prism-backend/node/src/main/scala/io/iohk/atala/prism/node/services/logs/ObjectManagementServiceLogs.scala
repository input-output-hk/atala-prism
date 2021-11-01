package io.iohk.atala.prism.node.services.logs

import cats.effect.MonadThrow
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.services.ObjectManagementService
import io.iohk.atala.prism.node.services.models._
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import tofu.syntax.monadic._
import cats.syntax.traverse._
import cats.syntax.applicativeError._
import io.iohk.atala.prism.node.services.ObjectManagementService.SaveObjectError

import java.time.Instant

private[services] class ObjectManagementServiceLogs[
    F[_]: ServiceLogging[*[_], ObjectManagementService[F]]: MonadThrow
] extends ObjectManagementService[Mid[F, *]] {
  def saveObject(
      notification: AtalaObjectNotification
  ): Mid[F, Either[SaveObjectError, Boolean]] =
    in =>
      info"saving object txId: ${notification.transaction.transactionId}, ledger: ${notification.transaction.ledger}" *> in
        .flatTap {
          _.fold(
            err => error"saving object - failed cause: $err",
            res => info"saving object - successfully done, result: $res"
          )
        }
        .onError(errorCause"Encountered an error while saving object" (_))

  def scheduleSingleAtalaOperation(
      op: SignedAtalaOperation
  ): Mid[F, Either[errors.NodeError, AtalaOperationId]] =
    in =>
      info"scheduling single atala operation" *> in
        .flatTap(logScheduleOperationResult)
        .onError(
          errorCause"Encountered an error while scheduling single atala operation" (
            _
          )
        )

  def scheduleAtalaOperations(
      ops: SignedAtalaOperation*
  ): Mid[F, List[Either[errors.NodeError, AtalaOperationId]]] =
    in =>
      info"scheduling atala operations, size - ${ops.size}" *> in
        .flatTap(_.traverse(logScheduleOperationResult))
        .onError(
          errorCause"Encountered an error while scheduling atala operations" (_)
        )

  def getLastSyncedTimestamp: Mid[F, Instant] =
    in =>
      info"getting last synced timestamp" *> in
        .flatTap(time => info"getting last synced timestamp - successfully done, got timestamp: $time")
        .onError(
          errorCause"Encountered an error while getting last synced timestamp" (
            _
          )
        )

  def getOperationInfo(
      atalaOperationId: AtalaOperationId
  ): Mid[F, Option[AtalaOperationInfo]] =
    in =>
      info"getting operation info by AtalaOperationId ${atalaOperationId.hexValue}" *> in
        .flatTap {
          _.fold(
            error"getting operation info by AtalaOperationId - finished with result: None"
          )(info => info"""getting operation info by AtalaOperationId - finished with result:
        "AtalaOperationId(${info.operationId.hexValue}), AtalaObjectId(${info.objectId.hexValue})""")
        }
        .onError(
          errorCause"Encountered an error while getting operation info by AtalaOperationId" (
            _
          )
        )

  private def logScheduleOperationResult(
      result: Either[errors.NodeError, AtalaOperationId]
  ): F[Unit] =
    result.fold(
      err => error"scheduling single atala operation - failed cause $err",
      id => info"scheduling single atala operation - successfully done, result: ${id.hexValue}"
    )
}
