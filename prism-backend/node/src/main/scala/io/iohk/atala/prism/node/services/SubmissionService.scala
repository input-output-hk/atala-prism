package io.iohk.atala.prism.node.services

import cats.data.EitherT
import cats.implicits.catsSyntaxEitherId
import cats.syntax.traverse._
import cats.effect.IO
import io.iohk.atala.prism.models.{TransactionInfo, TransactionStatus}
import io.iohk.atala.prism.node.UnderlyingLedger
import io.iohk.atala.prism.node.cardano.models.{CardanoWalletError, CardanoWalletErrorCode}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.{
  AtalaObjectInfo,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus
}
import io.iohk.atala.prism.node.repositories.{AtalaObjectsTransactionsRepository, AtalaOperationsRepository}
import io.iohk.atala.prism.node.services.SubmissionService.Config
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class SubmissionService private (
    config: Config,
    atalaReferenceLedger: UnderlyingLedger,
    atalaOperationsRepository: AtalaOperationsRepository[IO],
    atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[IO]
)(implicit scheduler: Scheduler) {
  type Result[A] = Future[Either[NodeError, A]]
  private val logger = LoggerFactory.getLogger(this.getClass)

  // Schedule first run
  scheduleRetryOldPendingTransactions(config.transactionRetryPeriod)

  private var submitReceivedObjectsTask: Option[monix.execution.Cancelable] = None
  scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)

  def flushOperationsBuffer(): Unit = {
    submitReceivedObjectsTask.fold(
      logger.info("Skip flushing because operations submission is already in progress.")
    ) { task =>
      task.cancel() // cancel a scheduled task
      scheduleSubmitReceivedObjects(config.operationSubmissionPeriod, immediate = true)
    }
  }

  def submitReceivedObjects(): Result[Unit] = {
    val submissionET = for {
      atalaObjects <- EitherT(atalaObjectsTransactionsRepository.getNotPublishedObjects.unsafeToFuture())
      _ = logger.info(s"Submit buffered objects. Number of objects: ${atalaObjects.size}")
      atalaObjectsMerged <- EitherT.right(mergeAtalaObjects(atalaObjects))
      atalaObjectsWithParsedContent = atalaObjectsMerged.map { obj => (obj, parseObjectContent(obj)) }
      _ <- EitherT.right[NodeError](publishObjectsAndRecordTransaction(atalaObjectsWithParsedContent))
    } yield ()

    submissionET.value
  }

  def retryOldPendingTransactions(): Future[Int] = {
    logger.info("Retry old pending transactions submission")
    val getOldPendingTransactions =
      atalaObjectsTransactionsRepository
        .getOldPendingTransactions(config.ledgerPendingTransactionTimeout, atalaReferenceLedger.getType)

    for {
      // Query old pending transactions
      pendingTransactions <- getOldPendingTransactions.unsafeToFuture()

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

  private def mergeAndRetryPendingTransactions(
      transactions: List[AtalaObjectTransactionSubmission]
  ): Future[Int] = {
    for {
      deletedTransactions <- deleteTransactions(transactions)
      atalaObjects <-
        atalaObjectsTransactionsRepository.retrieveObjects(deletedTransactions).map(_.flatten).unsafeToFuture()
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
      dbUpdateE <-
        atalaObjectsTransactionsRepository
          .updateSubmissionStatus(submission, newSubmissionStatus)
          .unsafeToFuture()
      _ = logger.info(s"Status for transaction [${submission.transactionId}] updated to $newSubmissionStatus")
    } yield for {
      transactionWithDetails <- transactionE
      _ <- dbUpdateE
    } yield transactionWithDetails
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

      _ <- EitherT(
        atalaObjectsTransactionsRepository
          .storeTransactionSubmission(atalaObjectInfo, publication)
          .unsafeToFuture()
      )
    } yield publication.transaction

    publicationEitherT.value
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
        atalaObjectsTransactionsRepository
          .updateSubmissionStatus(transaction, AtalaObjectTransactionSubmissionStatus.InLedger)
          .map { dbResultEither =>
            dbResultEither.left.map { err =>
              logger.error(s"Could not update status to InLedger for transaction ${transaction.transactionId}", err)
            }.toOption
          }
      }
      .map(_.flatten.size)
      .unsafeToFuture()
  }
}

object SubmissionService {
  case class Config(
      ledgerPendingTransactionTimeout: Duration,
      transactionRetryPeriod: FiniteDuration = 20.seconds,
      operationSubmissionPeriod: FiniteDuration = 20.seconds
  )

  def apply(
      config: Config,
      atalaReferenceLedger: UnderlyingLedger,
      atalaOperationsRepository: AtalaOperationsRepository[IO],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[IO]
  )(implicit scheduler: Scheduler): SubmissionService = {
    new SubmissionService(config, atalaReferenceLedger, atalaOperationsRepository, atalaObjectsTransactionsRepository)
  }
}
