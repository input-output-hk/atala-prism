package io.iohk.atala.prism.node.services

import cats.data.EitherT
import cats.implicits.catsSyntaxEitherId
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
import io.iohk.atala.prism.models.{TransactionInfo, TransactionStatus}
import io.iohk.atala.prism.node.{PublicationInfo, UnderlyingLedger}
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_TIMESTAMP
import io.iohk.atala.prism.node.cardano.models.{CardanoWalletError, CardanoWalletErrorCode}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.nodeState.getLastSyncedTimestampFromMaybe
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.repositories.AtalaOperationsRepository
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.{AtalaObjectCreateData, AtalaObjectSetTransactionInfo}
import io.iohk.atala.prism.node.repositories.daos.{
  AtalaObjectTransactionSubmissionsDAO,
  AtalaObjectsDAO,
  AtalaOperationsDAO,
  KeyValuesDAO
}
import io.iohk.atala.prism.node.repositories.utils.connectionIOSafe
import io.iohk.atala.prism.node.services.ObjectManagementService.Config
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_internal.AtalaBlock
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_internal, node_models}
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import java.time.{Duration, Instant}
import scala.concurrent.duration._
import scala.concurrent.Future

private class AtalaObjectCannotBeModified extends Exception
private class DuplicateAtalaBlock extends Exception

class ObjectManagementService private (
    config: Config,
    atalaReferenceLedger: UnderlyingLedger,
    atalaOperationsRepository: AtalaOperationsRepository[IO],
    blockProcessing: BlockProcessingService
)(implicit xa: Transactor[IO], scheduler: Scheduler) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  type Result[A] = Future[Either[NodeError, A]]

  // Schedule first run
  scheduleRetryOldPendingTransactions(config.transactionRetryPeriod)

  private var submitReceivedObjectsTask: Option[monix.execution.Cancelable] = None
  scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)

  private def dbTransactionSafe[T](connectionIO: ConnectionIO[T]): Result[T] =
    connectionIOSafe(connectionIO)
      .transact(xa)
      .unsafeToFuture()

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
        throw new RuntimeException(err.toString)
      },
      _ => atalaOperationIds
    )
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
      atalaObjectInfo: AtalaObjectInfo,
      atalaObject: node_internal.AtalaObject
  ): Result[TransactionInfo] = {
    logger.info(s"Publish atala object [${atalaObjectInfo.objectId}]")
    val publicationEitherT = for {
      // Publish object to the blockchain
      publication <- EitherT(
        atalaReferenceLedger.publish(atalaObject)
      ).leftMap(NodeError.InternalCardanoWalletError)

      _ <- EitherT(storeTransactionSubmission(atalaObjectInfo, publication))
    } yield publication.transaction

    publicationEitherT.value
  }

  private def storeTransactionSubmission(
      atalaObjectInfo: AtalaObjectInfo,
      publication: PublicationInfo
  ): Result[AtalaObjectTransactionSubmission] = {
    val query = AtalaObjectTransactionSubmissionsDAO
      .insert(
        AtalaObjectTransactionSubmission(
          atalaObjectInfo.objectId,
          publication.transaction.ledger,
          publication.transaction.transactionId,
          Instant.now,
          toAtalaObjectTransactionSubmissionStatus(publication.status)
        )
      )
      .logSQLErrors(s"publishing and record transaction for [${atalaObjectInfo.objectId}]", logger)

    dbTransactionSafe(query)
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
        .map { submissionResult =>
          submissionResult.left.foreach { err =>
            logger.error("Could not submit received objects", err)
          }
          ()
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
        .recover { err =>
          logger.error("Could not retry old pending transactions", err)
        }
        .onComplete { _ =>
          scheduleRetryOldPendingTransactions(config.transactionRetryPeriod)
        }
    }
    ()
  }

  private[services] def submitReceivedObjects(): Result[Unit] = {
    val submissionET = for {
      atalaObjects <- EitherT(atalaOperationsRepository.getNotPublishedObjects.unsafeToFuture())
      _ = logger.info(s"Submit buffered objects. Number of objects: ${atalaObjects.size}")
      atalaObjectsMerged <- EitherT.right(mergeAtalaObjects(atalaObjects))
      atalaObjectsWithParsedContent = atalaObjectsMerged.map { obj => (obj, parseObjectContent(obj)) }
      _ <- EitherT.right[NodeError](publishObjectsAndRecordTransaction(atalaObjectsWithParsedContent))
    } yield ()

    submissionET.value
  }

  private def getTransactionDetails(
      transaction: AtalaObjectTransactionSubmission
  ): Future[Option[(AtalaObjectTransactionSubmission, TransactionStatus)]] = {
    logger.info(s"Getting transaction details for transaction ${transaction.transactionId}")
    for {
      transactionDetails <- atalaReferenceLedger.getTransactionDetails(transaction.transactionId)
    } yield {
      transactionDetails.left
        .map { err =>
          logger.error("Could not get transaction details", err)
        }
        .map { transactionDetails =>
          (transaction, transactionDetails.status)
        }
        .toOption
    }
  }

  private def syncInLedgerTransactions(
      transactions: List[AtalaObjectTransactionSubmission]
  ): Future[Int] = {
    transactions
      .traverse { transaction =>
        val updateStatusDb = AtalaObjectTransactionSubmissionsDAO
          .updateStatus(
            transaction.ledger,
            transaction.transactionId,
            AtalaObjectTransactionSubmissionStatus.InLedger
          )
          .logSQLErrors("retry transaction if pending", logger)
        dbTransactionSafe(updateStatusDb).map { dbResultEither =>
          dbResultEither.left.map { err =>
            logger.error(s"Could not update status to InLedger for transaction ${transaction.transactionId}", err)
          }.toOption
        }
      }
      .map(_.flatten.size)
  }

  private[services] def retryOldPendingTransactions(): Future[Int] = {
    logger.info("Retry old pending transactions submission")
    val getOldPendingTransactions = AtalaObjectTransactionSubmissionsDAO
      .getBy(
        olderThan = Instant.now.minus(config.ledgerPendingTransactionTimeout),
        status = AtalaObjectTransactionSubmissionStatus.Pending,
        ledger = atalaReferenceLedger.getType
      )
      .logSQLErrors("retry old pending transactions", logger)

    for {
      // Query old pending transactions
      pendingTransactions <- dbTransactionSafe(getOldPendingTransactions)
        .map {
          case Left(err) =>
            logger.error("Could not get old pending transactions", err)
            List.empty
          case Right(transactions) =>
            transactions
        }

      transactionsWithDetails <-
        pendingTransactions
          .traverse(getTransactionDetails)
          .map(_.flatten)

      (inLedgerTransactions, pendingTransactions) = transactionsWithDetails.partitionMap {
        case (transaction, TransactionStatus.InLedger) =>
          Left(transaction)
        case (transaction, _) =>
          Right(transaction)
      }
      numInLedgerSynced <- syncInLedgerTransactions(inLedgerTransactions)

      numPublished <- mergeAndRetryPendingTransactions(pendingTransactions)
    } yield {
      logger.info(
        s"pending txs: ${pendingTransactions.size}; " +
          s"new inLedger txs: ${inLedgerTransactions.size}; " +
          s"inLedger txs synced with database: $numInLedgerSynced; " +
          s"published txs: $numPublished"
      )
      numPublished
    }
  }

  private def mergeAndRetryPendingTransactions(
      transactions: List[AtalaObjectTransactionSubmission]
  ): Future[Int] = {
    for {
      deletedTransactions <- deleteTransactions(transactions)
      atalaObjects <- atalaOperationsRepository.retrieveObjects(deletedTransactions).map(_.flatten).unsafeToFuture()
      atalaObjectsMerged <- mergeAtalaObjects(atalaObjects)
      atalaObjectsWithParsedContent = atalaObjectsMerged.map { obj => (obj, parseObjectContent(obj)) }
      publishedTransactions <- publishObjectsAndRecordTransaction(atalaObjectsWithParsedContent)
    } yield publishedTransactions.size
  }

  private def publishObjectsAndRecordTransaction(
      atalaObjectsWithParsedContent: List[(AtalaObjectInfo, node_internal.AtalaObject)]
  ): Future[List[TransactionInfo]] =
    atalaObjectsWithParsedContent
      .traverse {
        case (obj, objContent) =>
          publishAndRecordTransaction(obj, objContent).map { transactionInfoE =>
            transactionInfoE.left.map { err =>
              logger.error("Was not able to publish and record transaction", err)
            }.toOption
          }
      }
      .map(_.flatten)

  private def deleteTransactions(
      transactions: List[AtalaObjectTransactionSubmission]
  ): Future[List[AtalaObjectTransactionSubmission]] =
    Future
      .traverse(transactions) { transaction =>
        deleteTransactionMaybe(transaction).map(_.toOption)
      }
      .map(_.flatten)

  private def deleteTransactionMaybe(
      submission: AtalaObjectTransactionSubmission
  ): Result[AtalaObjectTransactionSubmission] = {
    logger.info(s"Trying to delete transaction [${submission.transactionId}]")
    for {
      (newSubmissionStatus, transactionE) <- atalaReferenceLedger.deleteTransaction(submission.transactionId).map {
        case Left(err @ CardanoWalletError(_, CardanoWalletErrorCode.TransactionAlreadyInLedger)) =>
          (
            AtalaObjectTransactionSubmissionStatus.InLedger,
            NodeError
              .InternalCardanoWalletError(err)
              .asLeft[AtalaObjectTransactionSubmission]
          )
        case Left(err) =>
          logger.error(s"Could not delete transaction ${submission.transactionId}", err)
          (submission.status, NodeError.InternalCardanoWalletError(err).asLeft[AtalaObjectTransactionSubmission])
        case Right(_) =>
          (AtalaObjectTransactionSubmissionStatus.Deleted, Right(submission))
      }
      dbUpdateE <- updateSubmissionStatus(submission, newSubmissionStatus)
      _ = logger.info(s"Status for transaction [${submission.transactionId}] updated to $newSubmissionStatus")
    } yield for {
      transactionWithDetails <- transactionE
      _ <- dbUpdateE
    } yield transactionWithDetails
  }

  private def updateSubmissionStatus(
      submission: AtalaObjectTransactionSubmission,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): Result[Unit] = {
    val opDescription = s"Setting status $newSubmissionStatus for transaction ${submission.transactionId}"
    val query = AtalaObjectTransactionSubmissionsDAO
      .updateStatus(
        submission.ledger,
        submission.transactionId,
        newSubmissionStatus
      )
      .logSQLErrors(opDescription, logger)
      .void

    if (submission.status != newSubmissionStatus)
      dbTransactionSafe(query)
    else
      Future.successful(().asRight[NodeError])
  }

  private def mergeAtalaObjects(
      atalaObjects: List[AtalaObjectInfo]
  ): Future[List[AtalaObjectInfo]] = {
    val atalaObjectsMerged =
      atalaObjects
        .foldRight(
          List.empty[(AtalaObjectInfo, List[AtalaObjectInfo])]
        ) {
          case (atalaObject, Nil) =>
            List((atalaObject, List(atalaObject)))
          case (atalaObject, lst @ (accObject, oldObjects) :: rest) =>
            atalaObject
              .mergeIfPossible(accObject)
              .fold((atalaObject, List(atalaObject)) :: lst) { mergedObject =>
                (mergedObject, atalaObject :: oldObjects) :: rest
              }
        }

    val objects = Future.traverse(atalaObjectsMerged) {
      case (atalaObject, oldObjects) =>
        if (oldObjects.size != 1) {
          val changedBlockE = atalaObject.getAndValidateAtalaObject
            .flatMap(_.blockContent)
            .toRight {
              NodeError.InternalError(s"Block in object ${atalaObject.objectId} was invalidated after merge.")
            }
          val atalaObjectFE = for {
            changedBlock <- EitherT.fromEither[Future](changedBlockE)
            _ <- EitherT(createAndUpdateAtalaObject(atalaObject, changedBlock.operations.toList, oldObjects))
          } yield atalaObject

          atalaObjectFE.value.map {
            case Left(err) =>
              logger.error(err.toString)
              None
            case Right(atalaObjectInfo) =>
              Some(atalaObjectInfo)
          }
        } else {
          Future.successful(Some(atalaObject))
        }
    }
    objects.map(_.flatten)
  }

  private def parseObjectContent(atalaObjectInfo: AtalaObjectInfo): node_internal.AtalaObject =
    atalaObjectInfo.getAndValidateAtalaObject.getOrElse {
      throw new RuntimeException(s"Can't extract AtalaObject content for objectId=${atalaObjectInfo.objectId}")
    }

  private def createAndUpdateAtalaObject(
      atalaObject: AtalaObjectInfo,
      operations: List[SignedAtalaOperation],
      oldObjects: List[AtalaObjectInfo]
  ): Result[Unit] =
    atalaOperationsRepository.updateMergedObjects(atalaObject, operations, oldObjects).unsafeToFuture()
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
      atalaOperationsRepository: AtalaOperationsRepository[IO],
      blockProcessing: BlockProcessingService
  )(implicit xa: Transactor[IO], scheduler: Scheduler): ObjectManagementService = {
    new ObjectManagementService(config, atalaReferenceLedger, atalaOperationsRepository, blockProcessing)
  }
}
