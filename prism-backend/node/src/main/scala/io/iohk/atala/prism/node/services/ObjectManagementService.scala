package io.iohk.atala.prism.node.services

import cats.effect.IO
import cats.syntax.functor._
import cats.syntax.traverse._
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import enumeratum.EnumEntry.Snakecase
import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.models.{TransactionDetails, TransactionInfo, TransactionStatus}
import io.iohk.atala.prism.node.UnderlyingLedger
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_TIMESTAMP
import io.iohk.atala.prism.node.models.nodeState.getLastSyncedTimestampFromMaybe
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.{AtalaObjectCreateData, AtalaObjectSetTransactionInfo}
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectTransactionSubmissionsDAO, AtalaObjectsDAO, AtalaOperationsDAO, KeyValuesDAO}
import io.iohk.atala.prism.node.services.ObjectManagementService.Config
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_internal.AtalaBlock
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import java.time.{Duration, Instant}
import scala.concurrent.Future
import scala.concurrent.duration._

private class AtalaObjectCannotBeModified extends Exception
private class DuplicateAtalaBlock extends Exception

class ObjectManagementService private (
    config: Config,
    atalaReferenceLedger: UnderlyingLedger,
    blockProcessing: BlockProcessingService
)(implicit xa: Transactor[IO], scheduler: Scheduler) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  // Schedule first run
  scheduleRetryOldPendingTransactions(config.transactionRetryPeriod)

  private var submitReceivedObjectsTask: Option[monix.execution.Cancelable] = None
  scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)

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
    val obj = node_internal.AtalaObject(blockOperationCount = block.operations.size).withBlockContent(block)
    val objBytes = obj.toByteArray
    val objId = AtalaObjectId.of(objBytes)

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

  private def processObject(obj: AtalaObjectInfo): Future[ConnectionIO[Boolean]] = {
    for {
      protobufObject <- Future.fromTry(node_internal.AtalaObject.validate(obj.byteContent))
      block = protobufObject.blockContent.get
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

  def flushOperationsBuffer(): Unit = {
    submitReceivedObjectsTask.fold(
      logger.info("Skip flushing because operations submission is already in progress.")
    ) { task =>
      task.cancel() // cancel a scheduled task
      scheduleSubmitReceivedObjects(config.operationSubmissionPeriod, immediate = true)
    }
  }

  private def scheduleSubmitReceivedObjects(delay: FiniteDuration, immediate: Boolean = false): Unit = {
    def run(): Unit = {
      submitReceivedObjectsTask = None
      // Ensure run is scheduled after completion, even if current run fails
      submitReceivedObjects()
        .recover {
          case e =>
            logger.error(s"Could not submit received objects", e)
        }
        .onComplete { _ =>
          scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)
        }
    }

    if (immediate) {
      run()
    } else {
      submitReceivedObjectsTask = Some(
        scheduler.scheduleOnce(delay)(run())
      )
    }
    ()
  }

  private def scheduleRetryOldPendingTransactions(delay: FiniteDuration): Unit = {
    scheduler.scheduleOnce(delay) {
      // Ensure run is scheduled after completion, even if current run fails
      retryOldPendingTransactions()
        .recover {
          case e =>
            logger.error(s"Could not retry old pending transactions", e)
        }
        .onComplete { _ =>
          scheduleRetryOldPendingTransactions(config.transactionRetryPeriod)
        }
    }
    ()
  }

  private[services] def submitReceivedObjects(): Future[Unit] = {
    val getNotPublishedObjects = for {
      objectIds <- AtalaObjectsDAO.getNotPublishedObjectIds
      objectInfos <- objectIds.traverse(AtalaObjectsDAO.get)
    } yield objectInfos.flatten

    for {
      atalaObjects <-
        getNotPublishedObjects
          .logSQLErrors(s"Extract not submitted objects.", logger)
          .transact(xa)
          .unsafeToFuture()
      _ = logger.info(s"Submit buffered objects. Number of objects: ${atalaObjects.size}")
      atalaObjectsMerged <- mergeAtalaObjects(atalaObjects)
      atalaObjectsWithParsedContent = atalaObjectsMerged.map { obj => (obj, parseObjectContent(obj)) }
      _ <- publishObjectsAndRecordTransaction(atalaObjectsWithParsedContent)
    } yield ()
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
      atalaObjectsWithParsedContent = atalaObjectsMerged.map { obj => (obj, parseObjectContent(obj)) }
      _ <- deleteTransactions(transactions)
      _ <- publishObjectsAndRecordTransaction(atalaObjectsWithParsedContent)
    } yield ()
  }

  private def publishObjectsAndRecordTransaction(
      atalaObjectsWithParsedContent: List[(AtalaObjectInfo, node_internal.AtalaObject)]
  ): Future[List[TransactionInfo]] =
    atalaObjectsWithParsedContent
      .traverse {
        case (obj, objContent) =>
          publishAndRecordTransaction(obj.objectId, objContent)
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
          val changedBlock = atalaObject.getAndValidateAtalaObject.flatMap(_.blockContent).getOrElse {
            throw new RuntimeException(s"Block in object ${atalaObject.objectId} was invalidated after merge.")
          }
          createAndUpdateAtalaObject(atalaObject, changedBlock.operations.toList)
            .map(_ => atalaObject)
        } else {
          Future.successful(atalaObject)
        }
    }
  }

  private def parseObjectContent(atalaObjectInfo: AtalaObjectInfo): node_internal.AtalaObject =
    atalaObjectInfo.getAndValidateAtalaObject.getOrElse {
      throw new RuntimeException(s"Can't extract AtalaObject content for objectId=${atalaObjectInfo.objectId}")
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
      transactionRetryPeriod: FiniteDuration = 20.seconds,
      operationSubmissionPeriod: FiniteDuration = 20.seconds
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
      atalaReferenceLedger: UnderlyingLedger,
      blockProcessing: BlockProcessingService
  )(implicit xa: Transactor[IO], scheduler: Scheduler): ObjectManagementService = {
    new ObjectManagementService(config, atalaReferenceLedger, blockProcessing)
  }
}
