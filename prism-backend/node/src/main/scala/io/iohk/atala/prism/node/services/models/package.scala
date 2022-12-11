package io.iohk.atala.prism.node.services

import cats.implicits._
import derevo.derive
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.operations.{
  CreateDIDOperation,
  DeactivateDIDOperation,
  IssueCredentialBatchOperation,
  ProtocolVersionUpdateOperation,
  RevokeCredentialsOperation,
  UpdateDIDOperation,
  ValidationError,
  parseOperationWithMockedLedger
}
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.protos.node_models.{OperationOutput, SignedAtalaOperation}
import tofu.logging.derivation.loggable

package object models {
  case class AtalaObjectNotification(
      atalaObject: node_internal.AtalaObject,
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
      case CreateDIDOperation(id, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.CreateDidOutput(
            node_models.CreateDIDOutput(id.getValue)
          )
        )
      case UpdateDIDOperation(_, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.UpdateDidOutput(
            node_models.UpdateDIDOutput()
          )
        )
      case IssueCredentialBatchOperation(credentialBatchId, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.BatchOutput(
            node_models.IssueCredentialBatchOutput(credentialBatchId.getId)
          )
        )
      case RevokeCredentialsOperation(_, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.RevokeCredentialsOutput(
            node_models.RevokeCredentialsOutput()
          )
        )
      case ProtocolVersionUpdateOperation(_, _, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.ProtocolVersionUpdateOutput(
            node_models.ProtocolVersionUpdateOutput()
          )
        )
      case DeactivateDIDOperation(_, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.DeactivateDidOutput(
            node_models.DeactivateDIDOutput()
          )
        )
      case other =>
        throw new IllegalArgumentException(
          "Unknown operation type: " + other.getClass
        )
    }

  type AtalaObjectNotificationHandler[F[_]] = AtalaObjectNotification => F[Unit]
}
