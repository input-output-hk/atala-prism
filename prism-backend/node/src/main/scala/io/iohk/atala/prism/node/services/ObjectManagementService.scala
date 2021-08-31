package io.iohk.atala.prism.node.services

import cats.data.EitherT
import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_TIMESTAMP
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.metrics.OperationsCounters
import io.iohk.atala.prism.node.models.nodeState.getLastSyncedTimestampFromMaybe
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.repositories.{
  AtalaObjectsTransactionsRepository,
  AtalaOperationsRepository,
  KeyValuesRepository
}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_internal.AtalaBlock
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import java.time.Instant
import scala.concurrent.Future

private class DuplicateAtalaBlock extends Exception

class ObjectManagementService private (
    atalaOperationsRepository: AtalaOperationsRepository[IO],
    atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[IO],
    keyValuesRepository: KeyValuesRepository[IO],
    blockProcessing: BlockProcessingService
)(implicit xa: Transactor[IO], scheduler: Scheduler) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  type Result[A] = Future[Either[NodeError, A]]

  def saveObject(notification: AtalaObjectNotification): Future[Unit] = {
    // TODO: just add the object to processing queue, instead of processing here
    atalaObjectsTransactionsRepository
      .setObjectTransactionDetails(notification)
      .flatMap {
        case Some(obj) =>
          processObject(obj).flatMap { transaction =>
            transaction
              .logSQLErrors("saving object", logger)
              .attemptSql
              .transact(xa)
              .map { resultEither =>
                resultEither.left.map { err =>
                  logger.warn(s"Could not process object $obj", err)
                }
                ()
              }
          }
        case None =>
          logger.warn(s"Could not save object from notification $notification")
          IO.unit
      }
      .unsafeToFuture()
  }

  def sendSingleAtalaOperation(op: node_models.SignedAtalaOperation): Future[AtalaOperationId] =
    sendAtalaOperations(op).map(_.head)

  def sendAtalaOperations(op: node_models.SignedAtalaOperation*): Future[List[AtalaOperationId]] = {
    val obj = ObjectManagementService.createAtalaObject(op.toList)
    val objBytes = obj.toByteArray
    val objId = AtalaObjectId.of(objBytes)

    val atalaOperations: List[SignedAtalaOperation] = op.toList
    val atalaOperationIds = atalaOperations.map(AtalaOperationId.of)

    val result = for {
      // Insert object into DB
      insertedCounts <- EitherT(
        atalaOperationsRepository
          .insertObjectAndOperations(objId, objBytes, atalaOperationIds, AtalaOperationStatus.RECEIVED)
          .unsafeToFuture()
      )
      (numInsertedObjects, numInsertedOperations) = insertedCounts
    } yield {
      if (numInsertedObjects == 0) {
        logger.warn(s"Object $objId was already received by PRISM Node")
        throw new DuplicateAtalaBlock()
      }
      if (numInsertedOperations != atalaOperationIds.size) {
        logger.warn(s"Some operations from object with id $objId was already received by PRISM node.")
      }
      atalaOperationIds
    }

    result.fold(
      { err =>
        OperationsCounters.failedToStoreToDbAtalaOperations(atalaOperations, err)
        throw new RuntimeException(err.toString)
      },
      { _ =>
        OperationsCounters.countReceivedAtalaOperations(atalaOperations)
        atalaOperationIds
      }
    )
  }

  def getLastSyncedTimestamp: Future[Instant] = {
    for {
      maybeLastSyncedBlockTimestamp <-
        keyValuesRepository
          .get(LAST_SYNCED_BLOCK_TIMESTAMP)
          .unsafeToFuture()
      lastSyncedBlockTimestamp = getLastSyncedTimestampFromMaybe(maybeLastSyncedBlockTimestamp.value)
    } yield lastSyncedBlockTimestamp
  }

  def getOperationInfo(atalaOperationId: AtalaOperationId): Future[Option[AtalaOperationInfo]] =
    atalaOperationsRepository
      .getOperationInfo(atalaOperationId)
      .unsafeToFuture()

  private def processObject(obj: AtalaObjectInfo): IO[ConnectionIO[Boolean]] = {
    for {
      protobufObject <- IO.fromTry(node_internal.AtalaObject.validate(obj.byteContent))
      block = protobufObject.blockContent.get
      transactionInfo = obj.transaction.getOrElse(throw new RuntimeException("AtalaObject has no transaction info"))
      transactionBlock =
        transactionInfo.block.getOrElse(throw new RuntimeException("AtalaObject has no transaction block"))
      _ = logBlockRequest("processObject", block, obj)
      blockProcessed = blockProcessing.processBlock(
        block,
        transactionInfo.transactionId,
        transactionInfo.ledger,
        transactionBlock.timestamp,
        transactionBlock.index
      )
    } yield for {
      wasProcessed <- blockProcessed
      _ <- AtalaObjectsDAO.updateObjectStatus(obj.objectId, AtalaObjectStatus.Processed)
    } yield wasProcessed
  }

  private def logBlockRequest(methodName: String, block: AtalaBlock, atalaObject: AtalaObjectInfo): Unit = {
    val operationIds = block.operations.map(AtalaOperationId.of).mkString("\n")
    logger.info(
      s"MethodName:$methodName \n Block OperationIds = [$operationIds \n] atalaObject = $atalaObject"
    )
  }
}

object ObjectManagementService {
  sealed trait AtalaObjectTransactionStatus extends EnumEntry with Snakecase
  object AtalaObjectTransactionStatus extends Enum[AtalaObjectTransactionStatus] {
    val values: IndexedSeq[AtalaObjectTransactionStatus] = findValues

    case object Pending extends AtalaObjectTransactionStatus
    case object InLedger extends AtalaObjectTransactionStatus
    case object Confirmed extends AtalaObjectTransactionStatus
  }

  case class AtalaObjectTransactionInfo(transaction: TransactionInfo, status: AtalaObjectTransactionStatus)

  def createAtalaObject(ops: List[SignedAtalaOperation]): node_internal.AtalaObject = {
    val block = node_internal.AtalaBlock(ATALA_OBJECT_VERSION, ops)
    node_internal.AtalaObject(blockOperationCount = block.operations.size).withBlockContent(block)
  }

  def apply(
      atalaOperationsRepository: AtalaOperationsRepository[IO],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[IO],
      keyValuesRepository: KeyValuesRepository[IO],
      blockProcessing: BlockProcessingService
  )(implicit xa: Transactor[IO], scheduler: Scheduler): ObjectManagementService = {
    new ObjectManagementService(
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      keyValuesRepository,
      blockProcessing
    )
  }
}
