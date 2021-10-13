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
import cats.syntax.applicativeError._

import java.time.Instant

private[services] class ObjectManagementServiceLogs[F[_]: ServiceLogging[*[_], ObjectManagementService[F]]: MonadThrow]
    extends ObjectManagementService[Mid[F, *]] {
  def saveObject(notification: AtalaObjectNotification): Mid[F, Unit] =
    in =>
      info"saving object $notification" *> in
        .flatTap(_ => info"saving object - successfully done")
        .onError(errorCause"Encountered an error while saving object" (_))

  def scheduleSingleAtalaOperation(op: SignedAtalaOperation): Mid[F, Either[errors.NodeError, AtalaOperationId]] =
    in =>
      info"scheduling single atala operation ${op.toProtoString}" *> in
        .flatTap(_ => info"scheduling single atala operation - successfully done")
        .onError(errorCause"Encountered an error while scheduling single atala operation" (_))

  def scheduleAtalaOperations(ops: SignedAtalaOperation*): Mid[F, List[Either[errors.NodeError, AtalaOperationId]]] =
    in =>
      info"scheduling atala operations, size - ${ops.size}" *> in
        .flatTap(_ => info"scheduling atala operations - successfully done")
        .onError(errorCause"Encountered an error while scheduling atala operations" (_))

  def getLastSyncedTimestamp: Mid[F, Instant] =
    in =>
      info"getting last synced timestamp" *> in
        .flatTap(_ => info"getting last synced timestamp - successfully done")
        .onError(errorCause"Encountered an error while getting last synced timestamp" (_))

  def getOperationInfo(atalaOperationId: AtalaOperationId): Mid[F, Option[AtalaOperationInfo]] =
    in =>
      info"getting operation info by AtalaOperationId $atalaOperationId" *> in
        .flatTap(_ => info"getting operation info by AtalaOperationId - successfully done")
        .onError(errorCause"Encountered an error while getting operation info by AtalaOperationId" (_))
}
