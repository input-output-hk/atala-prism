package io.iohk.atala.prism.node.metrics

import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.operations.StateError
import io.iohk.atala.prism.protos.node_models.AtalaOperation
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import kamon.Kamon
import kamon.metric.Metric
import kamon.tag.TagSet
import org.slf4j.LoggerFactory

import scala.util.Try

object OperationsCounters {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val SENT_OPERATION_METRIC_NAME = "sent-atala-operation"
  private val FAILED_TO_SEND_OPERATION_METRIC_NAME = "failed-to-send-atala-objects"
  private val PROCESSED_BLOCKS_METRIC_NAME = "processed-atala-blocks"
  private val FAILED_TO_PROCESS_BLOCKS_METRIC_NAME = "failed-to-process-atala-blocks"

  private val OPERATION_TAG = "atala-operation-type"
  private val NODE_ERROR_TAG = "node-error"
  private val STATE_ERROR_TAG = "state-error"

  private lazy val sentObjectsCounter: Metric.Counter = Kamon.counter(SENT_OPERATION_METRIC_NAME)
  private lazy val failedToSendObjectsCounter = Kamon.counter(FAILED_TO_SEND_OPERATION_METRIC_NAME)
  private lazy val processedBlocksCounter = Kamon.counter(PROCESSED_BLOCKS_METRIC_NAME)
  private lazy val failedToProcessBlocksCounter = Kamon.counter(FAILED_TO_PROCESS_BLOCKS_METRIC_NAME)

  def countSentAtalaOperations(in: List[SignedAtalaOperation]): Unit =
    increaseOperationsOccurrencesCounter(in, sentObjectsCounter, TagSet.builder())

  def countFailedToSendAtalaOperations(in: List[SignedAtalaOperation], error: NodeError): Unit =
    increaseOperationsOccurrencesCounter(
      in,
      failedToSendObjectsCounter,
      TagSet.builder().add(NODE_ERROR_TAG, nodeErrorToTagString(error))
    )

  def countProcessedBlock(op: SignedAtalaOperation): Unit =
    increaseBlockProcessedOperationsCounter(op.getOperation.operation, processedBlocksCounter, TagSet.builder())

  def countFailedToProcessBlock(op: SignedAtalaOperation, stateError: StateError): Unit =
    increaseBlockProcessedOperationsCounter(
      op.getOperation.operation,
      failedToProcessBlocksCounter,
      TagSet.builder().add(STATE_ERROR_TAG, stateErrorToTagString(stateError))
    )

  private def increaseBlockProcessedOperationsCounter(
      op: AtalaOperation.Operation,
      counter: Metric.Counter,
      tagSetBuilder: TagSet.Builder
  ): Unit =
    Try {
      val operationName = atalaOperationToTagString(op)
      counter.withTags(tagSetBuilder.add(OPERATION_TAG, operationName).build()).increment()
    }.toEither.left.foreach(error => logger.error(s"${counter.name} just blew up", error))

  private def increaseOperationsOccurrencesCounter(
      in: List[SignedAtalaOperation],
      counter: Metric.Counter,
      tagSetBuilder: TagSet.Builder
  ): Unit =
    Try {
      val counterStuff =
        in.map(_.getOperation).groupBy(op => atalaOperationToTagString(op.operation)).view.mapValues(_.size)
      counterStuff.foreach {
        case (operationName, occurrences) =>
          counter.withTags(tagSetBuilder.add(OPERATION_TAG, operationName).build()).increment(occurrences.toLong)
      }
    }.toEither.left.foreach(error => logger.error(s"${counter.name} counter just blew up", error))

  private def stateErrorToTagString: PartialFunction[StateError, String] = {
    case StateError.EntityExists(_, _) => "entity-exists"
    case StateError.InvalidSignature() => "invalid-signature"
    case StateError.EntityMissing(_, _) => "entity-missing"
    case StateError.InvalidPreviousOperation() => "invalid-previous-operation"
    case StateError.InvalidKeyUsed(_) => "invalid-key-used"
    case StateError.InvalidRevocation() => "invalid-revocation"
    case StateError.KeyAlreadyRevoked() => "key-already-revoked"
    case StateError.DuplicateOperation() => "duplicate-operation"
    case StateError.BatchAlreadyRevoked(_) => "batch-already-revoked"
    case StateError.UnknownKey(_, _) => "unknown-key"
  }

  private def nodeErrorToTagString: PartialFunction[NodeError, String] = {
    case NodeError.UnknownValueError(_, _) => "unknown-value"
    case NodeError.InternalError(_) => "internal"
    case NodeError.InternalCardanoWalletError(_) => "internal-cardano-wallet"
    case NodeError.InternalErrorDB(_) => "internal-db"
  }

  private def atalaOperationToTagString: PartialFunction[AtalaOperation.Operation, String] = {
    case AtalaOperation.Operation.Empty => "empty"
    case AtalaOperation.Operation.RevokeCredentials(_) => "revoke-credentials"
    case AtalaOperation.Operation.CreateDid(_) => "create-did"
    case AtalaOperation.Operation.UpdateDid(_) => "update-did"
    case AtalaOperation.Operation.IssueCredentialBatch(_) => "issue-credential-batch"
  }

}
