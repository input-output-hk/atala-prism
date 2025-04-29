package io.iohk.atala.prism.node.services

import cats.implicits._
import derevo.derive
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.TransactionInfo
import io.iohk.atala.prism.node.operations.CreateDIDOperation
import io.iohk.atala.prism.node.operations.DeactivateDIDOperation
import io.iohk.atala.prism.node.operations.ProtocolVersionUpdateOperation
import io.iohk.atala.prism.node.operations.UpdateDIDOperation
import io.iohk.atala.prism.node.operations.ValidationError
import io.iohk.atala.prism.node.operations.parseOperationWithMockedLedger
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.OperationOutput
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import tofu.logging.derivation.loggable

package object models {
  case class AtalaObjectNotification(
      atalaObject: node_models.AtalaObject,
      transaction: TransactionInfo
  )

  @derive(loggable)
  case class RefreshTransactionStatusesResult(pendingTransactions: Int, numInLedgerSynced: Int, numDeleted: Int)

  def validateScheduleOperationsRequest(operations: Seq[SignedAtalaOperation]): Either[NodeError, Unit] = {
    if (operations.isEmpty)
      NodeError.InvalidArgument("requirement failed: there must be at least one operation to be published").asLeft
    else
      ().asRight[NodeError]
  }

  def getOperationOutput(
      operation: SignedAtalaOperation
  ): Either[ValidationError, OperationOutput] =
    parseOperationWithMockedLedger(operation).map {
      case CreateDIDOperation(id, _, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.CreateDidOutput(
            node_api.CreateDIDOutput(id.getValue)
          )
        )
      case UpdateDIDOperation(_, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.UpdateDidOutput(
            node_api.UpdateDIDOutput()
          )
        )
      case ProtocolVersionUpdateOperation(_, _, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.ProtocolVersionUpdateOutput(
            node_api.ProtocolVersionUpdateOutput()
          )
        )
      case DeactivateDIDOperation(_, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.DeactivateDidOutput(
            node_api.DeactivateDIDOutput()
          )
        )
      case other =>
        throw new IllegalArgumentException(
          "Unknown operation type: " + other.getClass
        )
    }

  type AtalaObjectNotificationHandler[F[_]] = AtalaObjectNotification => F[Unit]
  type AtalaObjectBulkNotificationHandler[F[_]] = List[AtalaObjectNotification] => F[Unit]
}
