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
  private val RECEIVED_OPERATION_METRIC_NAME = "received-atala-operations"
  private val FAILED_TO_STORE_OPERATION_METRIC_NAME =
    "failed-to-store-atala-objects"
  private val PROCESSED_BLOCKS_METRIC_NAME = "processed-atala-block-operations"
  private val FAILED_TO_PROCESS_BLOCKS_METRIC_NAME =
    "failed-to-process-atala-block-operations"

  private val OPERATION_TAG = "atala-operation-type"
  private val UPDATE_SUB_OPERATION_TAG = "update-did-sub-operation"
  private val NODE_ERROR_TAG = "node-error"
  private val STATE_ERROR_TAG = "state-error"

  // Values for operations tags
  private val EMPTY_OPERATION_TAG_VALUE = "empty"
  private val REVOKE_CREDENTIALS_TAG_VALUE = "revoke-credentials"
  private val CREATE_DID_TAG_VALUE = "create-did"
  private val ISSUE_CREDENTIAL_BATCH_TAG_VALUE = "issue-credential-batch"
  private val PROTOCOL_VERSION_UPDATE_OPERATION_VALUE = "protocol-version-update"
  private val UPDATE_DID_OPERATION_TAG_VALUE = "did-update"
  private val DEACTIVATE_DID_TAG_VALUE = "deactivate-did"

  // Values for atala update did operations
  private val EMPTY_ACTION_TAG_VALUE = "empty-did-update"
  private val ADD_KEY_ACTION_TAG_VALUE = "add-key"
  private val REMOVE_KEY_ACTION_TAG_VALUE = "remove-key"
  private val ADD_SERVICE_ACTION_TAG_VALUE = "add-service"
  private val REMOVE_SERVICE_ACTION_TAG_VALUE = "remove-service"
  private val UPDATE_SERVICE_ACTION_TAG_VALUE = "update-service"

  private lazy val receivedObjectsCounter: Metric.Counter =
    Kamon.counter(RECEIVED_OPERATION_METRIC_NAME)
  private lazy val failedToStoreObjectsCounter =
    Kamon.counter(FAILED_TO_STORE_OPERATION_METRIC_NAME)
  private lazy val processedBlocksCounter =
    Kamon.counter(PROCESSED_BLOCKS_METRIC_NAME)
  private lazy val failedToProcessBlocksCounter =
    Kamon.counter(FAILED_TO_PROCESS_BLOCKS_METRIC_NAME)

  def countReceivedAtalaOperations(in: List[SignedAtalaOperation]): Either[Throwable, Unit] =
    increaseOperationsOccurrencesCounter(
      in,
      receivedObjectsCounter,
      TagSet.builder()
    )

  def failedToStoreToDbAtalaOperations(
      in: List[SignedAtalaOperation],
      error: NodeError
  ): Either[Throwable, Unit] =
    increaseOperationsOccurrencesCounter(
      in,
      failedToStoreObjectsCounter,
      TagSet.builder().add(NODE_ERROR_TAG, error.name)
    )

  def countOperationApplied(op: SignedAtalaOperation): Unit =
    increaseBlockOperationsCounter(
      op.getOperation.operation,
      processedBlocksCounter,
      TagSet.builder()
    )

  def countOperationRejected(
      op: SignedAtalaOperation,
      stateError: StateError
  ): Unit =
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
      counter
        .withTags(tagSetBuilder.add(OPERATION_TAG, operationName).build())
        .increment()
    }.toEither.left.foreach(error => logger.error(s"${counter.name} just blew up", error))

  private def increaseOperationsOccurrencesCounter(
      in: List[SignedAtalaOperation],
      counter: Metric.Counter,
      tagSetBuilder: TagSet.Builder
  ): Either[Throwable, Unit] = {
    val res = Try {
      val operationAndOccurrences =
        in.map(_.getOperation).groupBy(_.operation).view.mapValues(_.size)
      operationAndOccurrences.foreach { case (operation, occurrences) =>
        countAtalaDidOperations(operation, occurrences, counter, tagSetBuilder)
      }
    }.toEither

    res.left.foreach(error => logger.error(s"${counter.name} counter just blew up", error))
    res
  }

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
        countNonUpdateDidOperations(
          anythingElse,
          occurrences,
          counter,
          tagSetBuilder
        )
    }

  private def countDidUpdateOperations(
      actions: Seq[UpdateDIDAction],
      counter: Metric.Counter,
      tagSetBuilder: TagSet.Builder
  ): Unit = {
    val updateOperationTag =
      tagSetBuilder.add(OPERATION_TAG, UPDATE_DID_OPERATION_TAG_VALUE)
    actions
      .map(updateDidAction => atalaUpdateDidActionToTag(updateDidAction.action))
      .groupBy(identity)
      .view
      .foreach { case (subOperationName, subOperationsLists) =>
        counter
          .withTags(
            updateOperationTag
              .add(UPDATE_SUB_OPERATION_TAG, subOperationName)
              .build()
          )
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
    counter
      .withTags(tagSetBuilder.add(OPERATION_TAG, opTagValue).build())
      .increment(occurrences.toLong)
    ()
  }

  private def atalaOperationToTagString: PartialFunction[AtalaOperation.Operation, String] = {
    case AtalaOperation.Operation.Empty => EMPTY_OPERATION_TAG_VALUE
    case AtalaOperation.Operation.RevokeCredentials(_) =>
      REVOKE_CREDENTIALS_TAG_VALUE
    case AtalaOperation.Operation.CreateDid(_) => CREATE_DID_TAG_VALUE
    case AtalaOperation.Operation.DeactivateDid(_) => DEACTIVATE_DID_TAG_VALUE
    case AtalaOperation.Operation.ProtocolVersionUpdate(_) => PROTOCOL_VERSION_UPDATE_OPERATION_VALUE
    case AtalaOperation.Operation.IssueCredentialBatch(_) =>
      ISSUE_CREDENTIAL_BATCH_TAG_VALUE
    // Just in case, must be impossible
    case AtalaOperation.Operation.UpdateDid(_) => UPDATE_DID_OPERATION_TAG_VALUE
  }

  private def atalaUpdateDidActionToTag: PartialFunction[UpdateDIDAction.Action, String] = {
    case Action.Empty => EMPTY_ACTION_TAG_VALUE
    case Action.AddKey(_) => ADD_KEY_ACTION_TAG_VALUE
    case Action.RemoveKey(_) => REMOVE_KEY_ACTION_TAG_VALUE
    case Action.AddService(_) => ADD_SERVICE_ACTION_TAG_VALUE
    case Action.RemoveService(_) => REMOVE_SERVICE_ACTION_TAG_VALUE
    case Action.UpdateService(_) => UPDATE_SERVICE_ACTION_TAG_VALUE
  }

}
