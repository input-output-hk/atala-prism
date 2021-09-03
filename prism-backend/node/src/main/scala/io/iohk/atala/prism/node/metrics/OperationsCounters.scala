package io.iohk.atala.prism.node.metrics

import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.operations.StateError
import io.iohk.atala.prism.protos.node_models.{AtalaOperation, SignedAtalaOperation, UpdateDIDAction}
import io.iohk.atala.prism.protos.node_models.UpdateDIDAction.Action
import kamon.Kamon
import kamon.metric.Metric
import kamon.tag.TagSet
import org.slf4j.LoggerFactory

import scala.util.Try

object OperationsCounters {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val SENT_OPERATION_METRIC_NAME = "received-atala-operations"
  private val FAILED_TO_SEND_OPERATION_METRIC_NAME = "failed-to-store-atala-objects"
  private val PROCESSED_BLOCKS_METRIC_NAME = "processed-atala-block-operations"
  private val FAILED_TO_PROCESS_BLOCKS_METRIC_NAME = "failed-to-process-atala-block-operations"

  private val OPERATION_TAG = "atala-operation-type"
  private val UPDATE_SUB_OPERATION_TAG = "update-did-sub-operation"
  private val NODE_ERROR_TAG = "node-error"
  private val STATE_ERROR_TAG = "state-error"

  private lazy val sentObjectsCounter: Metric.Counter = Kamon.counter(SENT_OPERATION_METRIC_NAME)
  private lazy val failedToSendObjectsCounter = Kamon.counter(FAILED_TO_SEND_OPERATION_METRIC_NAME)
  private lazy val processedBlocksCounter = Kamon.counter(PROCESSED_BLOCKS_METRIC_NAME)
  private lazy val failedToProcessBlocksCounter = Kamon.counter(FAILED_TO_PROCESS_BLOCKS_METRIC_NAME)

  def countReceivedAtalaOperations(in: List[SignedAtalaOperation]): Unit =
    increaseOperationsOccurrencesCounter(in, sentObjectsCounter, TagSet.builder())

  def failedToStoreToDbAtalaOperations(in: List[SignedAtalaOperation], error: NodeError): Unit =
    increaseOperationsOccurrencesCounter(
      in,
      failedToSendObjectsCounter,
      TagSet.builder().add(NODE_ERROR_TAG, error.name)
    )

  def countOperationApplied(op: SignedAtalaOperation): Unit =
    increaseBlockOperationsCounter(op.getOperation.operation, processedBlocksCounter, TagSet.builder())

  def countOperationRejected(op: SignedAtalaOperation, stateError: StateError): Unit =
    increaseBlockOperationsCounter(
      op.getOperation.operation,
      failedToProcessBlocksCounter,
      TagSet.builder().add(STATE_ERROR_TAG, stateError.name)
    )

  private def increaseBlockOperationsCounter(
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
      val operationAndOccurrences = in.map(_.getOperation).groupBy(_.operation).view.mapValues(_.size)
      operationAndOccurrences.foreach {
        case (operation, occurrences) => countAtalaDidOperations(operation, occurrences, counter, tagSetBuilder)
      }
    }.toEither.left.foreach(error => logger.error(s"${counter.name} counter just blew up", error))

  private def countAtalaDidOperations(
      in: AtalaOperation.Operation,
      occurrences: Int,
      counter: Metric.Counter,
      tagSetBuilder: TagSet.Builder
  ): Unit =
    in match {
      // Since UpdateDid contains a list of actions - we want to count them one by one
      case AtalaOperation.Operation.UpdateDid(subOperation) =>
        countDidUpdateOperations(subOperation.actions, counter, tagSetBuilder)
      case anythingElse =>
        countNonUpdateDidOperations(anythingElse, occurrences, counter, tagSetBuilder)
    }

  private def countDidUpdateOperations(
      actions: Seq[UpdateDIDAction],
      counter: Metric.Counter,
      tagSetBuilder: TagSet.Builder
  ): Unit = {
    val updateOperationTag = tagSetBuilder.add(OPERATION_TAG, "update-did")
    actions
      .map(updateDidAction => atalaUpdateDidActionToTag(updateDidAction.action))
      .groupBy(identity)
      .view
      .foreach {
        case (subOperationName, subOperationsLists) =>
          counter
            .withTags(updateOperationTag.add(UPDATE_SUB_OPERATION_TAG, subOperationName).build())
            .increment(subOperationsLists.size.toLong)
      }
  }

  private def countNonUpdateDidOperations(
      in: AtalaOperation.Operation,
      occurrences: Int,
      counter: Metric.Counter,
      tagSetBuilder: TagSet.Builder
  ): Unit = {
    val opTagValue = atalaOperationToTagString(in)
    counter.withTags(tagSetBuilder.add(OPERATION_TAG, opTagValue).build()).increment(occurrences.toLong)
    ()
  }

  private def atalaOperationToTagString: PartialFunction[AtalaOperation.Operation, String] = {
    case AtalaOperation.Operation.Empty => "empty"
    case AtalaOperation.Operation.RevokeCredentials(_) => "revoke-credentials"
    case AtalaOperation.Operation.CreateDid(_) => "create-did"
    case AtalaOperation.Operation.IssueCredentialBatch(_) => "issue-credential-batch"
    // Just in case, must be impossible
    case AtalaOperation.Operation.UpdateDid(_) => "update-did"
  }

  private def atalaUpdateDidActionToTag: PartialFunction[UpdateDIDAction.Action, String] = {
    case Action.Empty => "empty-did-update"
    case Action.AddKey(_) => "add-key"
    case Action.RemoveKey(_) => "remove-key"
  }

}
