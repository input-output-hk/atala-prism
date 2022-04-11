package io.iohk.atala.prism.node.services.logs

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
import cats.MonadThrow

private[services] class ObjectManagementServiceLogs[
    F[_]: ServiceLogging[*[_], ObjectManagementService[F]]: MonadThrow
] extends ObjectManagementService[Mid[F, *]] {
  def saveObject(
      notification: AtalaObjectNotification
  ): Mid[F, Either[SaveObjectError, Boolean]] =
    in =>
      info"saving object included in transaction ${notification.transaction}" *> in
        .flatTap {
          _.fold(
            err => error"saving object - failed cause: $err",
            res => info"saving object - successfully done, result: $res"
          )
        }
        .onError(errorCause"Encountered an error while saving object" (_))

  def scheduleAtalaOperations(
      ops: SignedAtalaOperation*
  ): Mid[F, List[Either[errors.NodeError, AtalaOperationId]]] =
    in =>
      info"scheduling Atala operations, size - ${ops.size}" *> in
        .flatTap(_.traverse(logScheduleOperationResult))
        .onError(
          errorCause"Encountered an error while scheduling Atala operations" (_)
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

  def getCurrentProtocolVersion: Mid[F, ProtocolVersion] =
    in =>
      info"getting current protocol version" *> in
        .flatTap(protocolVersion =>
          info"getting current protocol version - successfully done, got protocol version: $protocolVersion"
        )
        .onError(
          errorCause"Encountered an error while getting current protocol version" (
            _
          )
        )

  def getOperationInfo(
      atalaOperationId: AtalaOperationId
  ): Mid[F, Option[AtalaOperationInfo]] =
    in =>
      info"getting operation info by $atalaOperationId" *> in
        .flatTap {
          _.fold(
            error"getting operation info by AtalaOperationId - finished with result: None"
          )(info => info"""getting operation info by AtalaOperationId - finished with result:
        "${info.operationId}, ${info.objectId}""")
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
      err => error"scheduling single Atala operation - failed cause: $err",
      id => info"scheduling single Atala operation - successfully done, result: $id"
    )

  override def getScheduledAtalaObjects(): Mid[F, Either[errors.NodeError, List[AtalaObjectInfo]]] = {
    val description = s"getting not processed Atala objects"
    in =>
      info"$description" *> in
        .flatTap {
          _.fold(
            err => error"Encountered an error while $description $err",
            ret => info"$description - successfully got ${ret.size} objects"
          )
        }
        .onError(
          errorCause"Encountered an error while $description" (
            _
          )
        )
  }
}
