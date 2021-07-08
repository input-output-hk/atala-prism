package io.iohk.atala.prism.node.services

import java.time.{Duration, Instant}
import cats.effect.IO
import cats.syntax.traverse._
import cats.syntax.functor._
import com.google.protobuf.ByteString
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{TransactionDetails, TransactionInfo, TransactionStatus}
import io.iohk.atala.prism.node.UnderlyingLedger
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_TIMESTAMP
import io.iohk.atala.prism.node.models.nodeState.getLastSyncedTimestampFromMaybe
import io.iohk.atala.prism.node.models.{
  AtalaObjectInfo,
  AtalaObjectId,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus,
  AtalaOperationInfo,
  AtalaOperationStatus
}
import io.iohk.atala.prism.node.objects.ObjectStorageService
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.{AtalaObjectCreateData, AtalaObjectSetTransactionInfo}
import io.iohk.atala.prism.node.repositories.daos.{
  AtalaObjectTransactionSubmissionsDAO,
  AtalaObjectsDAO,
  AtalaOperationsDAO,
  KeyValuesDAO
}
import io.iohk.atala.prism.node.services.ObjectManagementService.Config
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_internal.AtalaBlock
import io.iohk.atala.prism.protos.node_internal.AtalaObject.Block
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.execution.Scheduler
import org.slf4j.LoggerFactory
import scala.concurrent.Future
import scala.concurrent.duration._

private class AtalaObjectCannotBeModified extends Exception
private class DuplicateAtalaBlock extends Exception

class ObjectManagementService private (
    config: Config,
    storage: ObjectStorageService,
    atalaReferenceLedger: UnderlyingLedger,
    blockProcessing: BlockProcessingService
)(implicit xa: Transactor[IO], scheduler: Scheduler) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  // Schedule first run
  scheduleRetryOldPendingTransactions(config.ledgerPendingTransactionSyncDelay)

  private def setObjectTransactionDetails(notification: AtalaObjectNotification): Future[Option[AtalaObjectInfo]] = {
    val objectBytes = notification.atalaObject.toByteArray
    val objId = AtalaObjectId.of(objectBytes)

    val query = for {
      existingObject <- AtalaObjectsDAO.get(objId)
      _ <- {
        existingObject match {
          // Object previously found in the blockchain
          case Some(obj) if obj.transaction.isDefined => connection.raiseError(new AtalaObjectCannotBeModified)
          // Object previously saved in DB, but not in the blockchain
          case Some(_) => connection.unit
          // Object was not in DB, save it to populate transaction data below
          case None => AtalaObjectsDAO.insert(AtalaObjectCreateData(objId, objectBytes))
        }
      }

      _ = notification.transaction.block.getOrElse(
        throw new IllegalArgumentException("Transaction has no block")
      )
      _ <- AtalaObjectsDAO.setTransactionInfo(
        AtalaObjectSetTransactionInfo(
          objId,
          notification.transaction
        )
      )
      obj <- AtalaObjectsDAO.get(objId)
    } yield obj

    query
      .logSQLErrors("setting object transaction details", logger)
      .transact(xa)
      .unsafeToFuture()
      .recover {
        case _: AtalaObjectCannotBeModified => None
      }
  }

  def saveObject(notification: AtalaObjectNotification): Future[Unit] = {
    // TODO: just add the object to processing queue, instead of processing here
    setObjectTransactionDetails(notification)
      .flatMap {
        case Some(obj) =>
          processObject(obj).flatMap { transaction =>
            transaction.logSQLErrors("saving object", logger).transact(xa).unsafeToFuture().void
          } recover {
            case error => logger.warn(s"Could not process object $obj", error)
          }
        case None =>
          logger.warn(s"Could not save object from notification $notification")
          Future.successful(())
      }
  }

  def publishSingleAtalaOperation(op: node_models.SignedAtalaOperation): Future[AtalaOperationId] =
    publishAtalaOperations(op).map(_.head)

  def publishAtalaOperations(op: node_models.SignedAtalaOperation*): Future[List[AtalaOperationId]] = {
    val block = node_internal.AtalaBlock("1.0", op.toList)
    val blockBytes = block.toByteArray
    val blockHash = SHA256Digest.compute(blockBytes)
    val objectBlock =
      if (atalaReferenceLedger.supportsOnChainData) Block.BlockContent(block)
      else Block.BlockHash(ByteString.copyFrom(blockHash.value.toArray))
    val obj = node_internal.AtalaObject(block = objectBlock, blockOperationCount = block.operations.size)
    val objBytes = obj.toByteArray
    val objId = AtalaObjectId.of(objBytes)

    def storeDataOffChain(): Future[Unit] = {
      if (atalaReferenceLedger.supportsOnChainData) {
        // No need to store off-chain as whole object is in the chain already
        Future.unit
      } else {
        // Store object and block in off-chain storage
        storage.put(blockHash.hexValue, blockBytes)
      }
    }

    val atalaOperationIds = block.operations.toList.map(AtalaOperationId.of)
    val atalaOperationData = atalaOperationIds.map((_, objId, AtalaOperationStatus.RECEIVED))

    val insertObjectAndOps = for {
      insertObject <- AtalaObjectsDAO.insert(AtalaObjectCreateData(objId, objBytes))
      insertOperations <- AtalaOperationsDAO.insertMany(atalaOperationData)
    } yield {
      if (insertObject == 0) {
        connection.raiseError(throw new DuplicateAtalaBlock())
      }
      (insertObject, insertOperations)
    }

    for {
      // Insert object into DB
      insertedCounts <-
        insertObjectAndOps
          .logSQLErrors(
            s"inserting object and operations \n Operations:[${atalaOperationIds.mkString("\n")}]",
            logger
          )
          .transact(xa)
          .unsafeToFuture()
      (_, insertedOperationsCount) = insertedCounts
      // If the ledger does not support data on-chain, then store it off-chain
      _ <- storeDataOffChain()
      // Publish object to the blockchain
      _ <- publishAndRecordTransaction(objId, obj).void
    } yield {
      if (insertedOperationsCount != atalaOperationIds.size) {
        logger.warn(s"Some operations from object with id $objId was already received by PRISM node.")
      }
      atalaOperationIds
    }
  }

  def getLastSyncedTimestamp: Future[Instant] = {
    for {
      maybeLastSyncedBlockTimestamp <-
        KeyValuesDAO
          .get(LAST_SYNCED_BLOCK_TIMESTAMP)
          .logSQLErrors(s"getting key - $LAST_SYNCED_BLOCK_TIMESTAMP", logger)
          .transact(xa)
          .unsafeToFuture()
      lastSyncedBlockTimestamp = getLastSyncedTimestampFromMaybe(maybeLastSyncedBlockTimestamp)
    } yield lastSyncedBlockTimestamp
  }

  def getOperationInfo(atalaOperationId: AtalaOperationId): Future[Option[AtalaOperationInfo]] = {
    AtalaOperationsDAO
      .getAtalaOperationInfo(atalaOperationId)
      .logSQLErrors(s"getting operation info for [$atalaOperationId]", logger)
      .transact(xa)
      .unsafeToFuture()
  }

  private def publishAndRecordTransaction(
      atalaObjectId: AtalaObjectId,
      atalaObject: node_internal.AtalaObject
  ): Future[TransactionInfo] = {
    for {
      // Publish object to the blockchain
      publication <- atalaReferenceLedger.publish(atalaObject)
      // Store transaction submission
      _ <-
        AtalaObjectTransactionSubmissionsDAO
          .insert(
            AtalaObjectTransactionSubmission(
              atalaObjectId,
              publication.transaction.ledger,
              publication.transaction.transactionId,
              Instant.now,
              toAtalaObjectTransactionSubmissionStatus(publication.status)
            )
          )
          .logSQLErrors(s"publishing and record transaction for [$atalaObjectId]", logger)
          .transact(xa)
          .unsafeToFuture()
    } yield publication.transaction
  }

  private def toAtalaObjectTransactionSubmissionStatus(
      status: TransactionStatus
  ): AtalaObjectTransactionSubmissionStatus = {
    status match {
      case TransactionStatus.Pending => AtalaObjectTransactionSubmissionStatus.Pending
      case TransactionStatus.InLedger => AtalaObjectTransactionSubmissionStatus.InLedger
    }
  }

  private def getBlockFromObject(obj: node_internal.AtalaObject): Future[node_internal.AtalaBlock] = {
    obj.block match {
      case node_internal.AtalaObject.Block.BlockContent(block) => Future.successful(block)
      case node_internal.AtalaObject.Block.BlockHash(hash) =>
        storage
          .get(SHA256Digest.fromVectorUnsafe(hash.toByteArray.toVector).hexValue)
          .map(_.getOrElse(throw new RuntimeException(s"Content of block $hash not found")))
          .map(node_internal.AtalaBlock.parseFrom)
      case node_internal.AtalaObject.Block.Empty =>
        throw new IllegalStateException("Block has neither block content nor block hash")
    }
  }

  private def processObject(obj: AtalaObjectInfo): Future[ConnectionIO[Boolean]] = {
    for {
      protobufObject <- Future.fromTry(node_internal.AtalaObject.validate(obj.byteContent))
      block <- getBlockFromObject(protobufObject)
      transactionInfo = obj.transaction.getOrElse(throw new RuntimeException("AtalaObject has no transaction info"))
      transactionBlock =
        transactionInfo.block.getOrElse(throw new RuntimeException("AtalaObject has no transaction block"))
      _ = logBlockRequest("processObject", block, obj)
      blockProcess = blockProcessing.processBlock(
        block,
        transactionInfo.transactionId,
        transactionInfo.ledger,
        transactionBlock.timestamp,
        transactionBlock.index
      )
    } yield for {
      wasProcessed <- blockProcess
      _ <- AtalaObjectsDAO.setProcessed(obj.objectId)
    } yield wasProcessed
  }

  private def logBlockRequest(methodName: String, block: AtalaBlock, atalaObject: AtalaObjectInfo): Unit = {
    val operationIds = block.operations.map(AtalaOperationId.of).mkString("\n")
    logger.info(
      s"MethodName:$methodName \n Block OperationIds = [$operationIds \n] atalaObject = $atalaObject"
    )
  }
  private def scheduleRetryOldPendingTransactions(delay: FiniteDuration): Unit = {
    scheduler.scheduleOnce(delay) {
      // Ensure run is scheduled after completion, even if current run fails
      retryOldPendingTransactions()
        .recover {
          case e =>
            logger.error(s"Could not retry old pending transactions", e)
            false
        }
        .onComplete { _ =>
          scheduleRetryOldPendingTransactions(config.ledgerPendingTransactionSyncDelay)
        }
    }
    ()
  }

  private[services] def retryOldPendingTransactions(): Future[Unit] = {
    for {
      // Query old pending transactions
      pendingTransactions <-
        AtalaObjectTransactionSubmissionsDAO
          .getBy(
            olderThan = Instant.now.minus(config.ledgerPendingTransactionTimeout),
            status = AtalaObjectTransactionSubmissionStatus.Pending,
            ledger = atalaReferenceLedger.getType
          )
          .logSQLErrors("retry old pending transactions", logger)
          .transact(xa)
          .unsafeToFuture()

      transactionsWithDetails <- Future.traverse(pendingTransactions) { transaction =>
        atalaReferenceLedger
          .getTransactionDetails(transaction.transactionId)
          .map((transaction, _))
      }

      (inLedgerTransactions, pendingTransactions) = transactionsWithDetails.partition {
        case (_, transactionDetails) =>
          transactionDetails.status == TransactionStatus.InLedger
      }

      _ <- Future.traverse(inLedgerTransactions) {
        case (transaction, _) =>
          AtalaObjectTransactionSubmissionsDAO
            .updateStatus(
              transaction.ledger,
              transaction.transactionId,
              AtalaObjectTransactionSubmissionStatus.InLedger
            )
            .logSQLErrors("retry transaction if pending", logger)
            .transact(xa)
            .unsafeToFuture()
      }

      _ <- mergeAndRetryPendingTransactions(pendingTransactions)
    } yield ()
  }

  private def mergeAndRetryPendingTransactions(
      transactions: List[(AtalaObjectTransactionSubmission, TransactionDetails)]
  ): Future[Unit] = {
    for {
      atalaObjects <- retrieveObjects(transactions)
      atalaObjectsMerged <- mergeAtalaObjects(atalaObjects)
      atalaObjectsWithParsedContent = atalaObjectsMerged.map { obj =>
        val parsedContent = obj.getAndValidateAtalaObject.getOrElse {
          throw new RuntimeException(s"Can't extract AtalaObject content.")
        }
        (obj, parsedContent)
      }
      _ <- deleteTransactions(transactions)
      _ <-
        atalaObjectsWithParsedContent
          .traverse {
            case (obj, objContent) =>
              publishAndRecordTransaction(obj.objectId, objContent)
          }
    } yield ()
  }

  private def deleteTransactions(
      transactions: List[(AtalaObjectTransactionSubmission, TransactionDetails)]
  ): Future[Unit] = {
    Future
      .traverse(transactions) {
        case (transaction, _) =>
          atalaReferenceLedger.deleteTransaction(transaction.transactionId)
          AtalaObjectTransactionSubmissionsDAO
            .updateStatus(
              transaction.ledger,
              transaction.transactionId,
              AtalaObjectTransactionSubmissionStatus.Deleted
            )
            .logSQLErrors(s"Setting status Deleted for transaction ${transaction.transactionId}", logger)
            .transact(xa)
            .unsafeToFuture()
      }
      .void
  }

  private def retrieveObjects(
      transactions: List[(AtalaObjectTransactionSubmission, TransactionDetails)]
  ): Future[List[AtalaObjectInfo]] = {
    Future
      .traverse(transactions) {
        case (transaction, _) =>
          AtalaObjectsDAO
            .get(transaction.atalaObjectId)
            .logSQLErrors(s"Getting atala object by atalaObjectId = ${transaction.atalaObjectId}", logger)
            .transact(xa)
            .unsafeToFuture()
            .map { atalaObject =>
              atalaObject.getOrElse(
                throw new RuntimeException(s"Atala object with id ${transaction.atalaObjectId} not found")
              )
            }
      }
  }

  private def mergeAtalaObjects(atalaObjects: List[AtalaObjectInfo]): Future[List[AtalaObjectInfo]] = {
    val atalaObjectsMerged = atalaObjects.foldRight(List.empty[(AtalaObjectInfo, Boolean)]) {
      case (atalaObject, Nil) =>
        List((atalaObject, false))
      case (atalaObject, lst @ (accObject, _) :: rest) =>
        atalaObject
          .mergeIfPossible(accObject)
          .fold((atalaObject, false) :: lst) { mergedObject =>
            (mergedObject, true) :: rest
          }
    }

    Future.traverse(atalaObjectsMerged) {
      case (atalaObject, changed) =>
        if (changed) {
          val changedBlock = atalaObject.getAndValidateAtalaObject.flatMap(_.block.blockContent).getOrElse {
            throw new RuntimeException(s"Block in object ${atalaObject.objectId} was invalidated after merge.")
          }
          createAndUpdateAtalaObject(atalaObject, changedBlock.operations.toList)
            .map(_ => atalaObject)
        } else {
          Future.successful(atalaObject)
        }
    }
  }

  private def createAndUpdateAtalaObject(
      atalaObject: AtalaObjectInfo,
      operations: List[SignedAtalaOperation]
  ): Future[Unit] = {
    val query = for {
      _ <- AtalaObjectsDAO.insert(AtalaObjectCreateData(atalaObject.objectId, atalaObject.byteContent))
      _ <- AtalaOperationsDAO.updateAtalaOperationObjectBatch(
        operations.map(AtalaOperationId.of),
        atalaObject.objectId
      )
    } yield ()

    query
      .logSQLErrors(s"record new Atala Object ${atalaObject.objectId}", logger)
      .transact(xa)
      .unsafeToFuture()
  }

}

object ObjectManagementService {
  case class Config(
      ledgerPendingTransactionTimeout: Duration,
      ledgerPendingTransactionSyncDelay: FiniteDuration = 20.seconds
  )

  sealed trait AtalaObjectTransactionStatus extends EnumEntry with Snakecase
  object AtalaObjectTransactionStatus extends Enum[AtalaObjectTransactionStatus] {
    val values: IndexedSeq[AtalaObjectTransactionStatus] = findValues

    case object Pending extends AtalaObjectTransactionStatus
    case object InLedger extends AtalaObjectTransactionStatus
    case object Confirmed extends AtalaObjectTransactionStatus
  }

  case class AtalaObjectTransactionInfo(transaction: TransactionInfo, status: AtalaObjectTransactionStatus)

  def apply(
      config: Config,
      storage: ObjectStorageService,
      atalaReferenceLedger: UnderlyingLedger,
      blockProcessing: BlockProcessingService
  )(implicit xa: Transactor[IO], scheduler: Scheduler): ObjectManagementService = {
    new ObjectManagementService(config, storage, atalaReferenceLedger, blockProcessing)
  }
}
