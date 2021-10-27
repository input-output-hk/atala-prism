package io.iohk.atala.prism.node.services

import cats.{Comonad, Functor, Monad}
import cats.data.EitherT
import cats.effect.MonadThrow
import cats.syntax.applicative._
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.syntax.flatMap._
import cats.syntax.option._
import derevo.derive
import derevo.tagless.applyK
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
import io.iohk.atala.prism.node.services.logs.SubmissionServiceLogs
import io.iohk.atala.prism.protos.node_internal
import org.slf4j.LoggerFactory
import tofu.Execute
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}

import java.time.Duration

@derive(applyK)
trait SubmissionService[F[_]] {

  def submitReceivedObjects(): F[Either[NodeError, Unit]]

  def retryOldPendingTransactions(
      ledgerPendingTransactionTimeout: Duration
  ): F[Int]

}

object SubmissionService {

  case class Config(
      maxNumberTransactionsToSubmit: Int,
      maxNumberTransactionsToRetry: Int
  )

  def apply[F[_]: MonadThrow: Execute, R[_]: Functor](
      atalaReferenceLedger: UnderlyingLedger,
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      config: Config = Config(Int.MaxValue, Int.MaxValue),
      logs: Logs[R, F]
  ): R[SubmissionService[F]] =
    for {
      serviceLogs <- logs.service[SubmissionService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, SubmissionService[F]] =
        serviceLogs
      val logs: SubmissionService[Mid[F, *]] = new SubmissionServiceLogs[F]
      val mid = logs
      mid attach new SubmissionServiceImpl[F](
        atalaReferenceLedger,
        atalaOperationsRepository,
        atalaObjectsTransactionsRepository,
        config
      )
    }

  def unsafe[F[_]: MonadThrow: Execute, R[_]: Comonad](
      atalaReferenceLedger: UnderlyingLedger,
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      config: Config = Config(Int.MaxValue, Int.MaxValue),
      logs: Logs[R, F]
  ): SubmissionService[F] =
    SubmissionService(
      atalaReferenceLedger,
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      config,
      logs
    ).extract
}

private class SubmissionServiceImpl[F[_]: Monad](
    atalaReferenceLedger: UnderlyingLedger,
    atalaOperationsRepository: AtalaOperationsRepository[F],
    atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
    config: Config
)(implicit ex: Execute[F])
    extends SubmissionService[F] {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def submitReceivedObjects(): F[Either[NodeError, Unit]] = {
    val submissionET = for {
      atalaObjects <- EitherT(
        atalaObjectsTransactionsRepository.getNotPublishedObjects
      )
      _ = logger.info(
        s"Submit buffered objects. Number of objects: ${atalaObjects.size}"
      )
      atalaObjectsMerged <- EitherT.right(mergeAtalaObjects(atalaObjects))
      atalaObjectsWithParsedContent = atalaObjectsMerged.map { obj =>
        (obj, parseObjectContent(obj))
      }
      publishedTransactions <-
        EitherT.right[NodeError](
          publishObjectsAndRecordTransaction(atalaObjectsWithParsedContent)
        )
    } yield {
      logger.info(
        s"successfully published transactions: ${publishedTransactions.size}"
      )
    }

    submissionET.value
  }

  def retryOldPendingTransactions(
      ledgerPendingTransactionTimeout: Duration
  ): F[Int] = {
    logger.info("Retry old pending transactions submission")
    val getOldPendingTransactions =
      atalaObjectsTransactionsRepository
        .getOldPendingTransactions(
          ledgerPendingTransactionTimeout,
          atalaReferenceLedger.getType
        )

    for {
      // Query old pending transactions
      pendingTransactions <- getOldPendingTransactions

      transactionsWithDetails <-
        pendingTransactions
          .traverse(getTransactionDetails)
          .map(_.flatten)

      (inLedgerTransactions, notInLedgerTransactions) = transactionsWithDetails
        .partitionMap {
          case (transaction, TransactionStatus.InLedger) =>
            Left(transaction)
          case txWithStatus =>
            Right(txWithStatus)
        }
      numInLedgerSynced <- syncInLedgerTransactions(inLedgerTransactions)

      transactionsToRetry = notInLedgerTransactions.collect {
        case (transaction, status) if status != TransactionStatus.Submitted =>
          transaction
      }

      numPublished <- mergeAndRetryPendingTransactions(transactionsToRetry)
    } yield {
      logger.info(
        s"methodName: retryOldPendingTransactions , pending transactions: ${pendingTransactions.size}; " +
          s"InLedger transactions synced with database: $numInLedgerSynced; " +
          s"successfully retried transactions: $numPublished"
      )
      numPublished
    }
  }

  private def mergeAndRetryPendingTransactions(
      transactions: List[AtalaObjectTransactionSubmission]
  ): F[Int] = {
    for {
      deletedTransactions <- deleteTransactions(transactions)
      atalaObjects <-
        atalaObjectsTransactionsRepository
          .retrieveObjects(deletedTransactions)
          .map(_.flatten)
      atalaObjectsMerged <- mergeAtalaObjects(atalaObjects)
      atalaObjectsWithParsedContent = atalaObjectsMerged.map { obj =>
        (obj, parseObjectContent(obj))
      }
      publishedTransactions <- publishObjectsAndRecordTransaction(
        atalaObjectsWithParsedContent
      )
    } yield publishedTransactions.size
  }

  private def publishObjectsAndRecordTransaction(
      atalaObjectsWithParsedContent: List[(AtalaObjectInfo, node_internal.AtalaObject)]
  ): F[List[TransactionInfo]] = {
    def logAndKeep(keep: List[TransactionInfo])(err: NodeError): List[TransactionInfo] = {
      logger.error("Was not able to publish and record transaction", err)
      keep
    }

    atalaObjectsWithParsedContent
      .take(config.maxNumberTransactionsToSubmit)
      .foldLeft(Monad[F].pure(List.empty[TransactionInfo])) { case (accF, (obj, objContent)) =>
        for {
          acc <- accF
          transactionInfoE <- publishAndRecordTransaction(obj, objContent)
        } yield {
          transactionInfoE.fold(logAndKeep(acc), _ :: acc)
        }
      }
      .map(_.reverse)
  }

  private def deleteTransactions(
      transactions: List[AtalaObjectTransactionSubmission]
  ): F[List[AtalaObjectTransactionSubmission]] =
    transactions
      .traverse(deleteTransactionMaybe(_).map(_.toOption))
      .map(_.flatten)

  private def deleteTransactionMaybe(
      submission: AtalaObjectTransactionSubmission
  ): F[Either[NodeError, AtalaObjectTransactionSubmission]] = {
    logger.info(s"Trying to delete transaction [${submission.transactionId}]")
    for {
      deletionResult <-
        ex.deferFuture(
          atalaReferenceLedger.deleteTransaction(submission.transactionId)
        ).map(handleTransactionDeletion(submission, _))
      dbUpdateE <-
        atalaObjectsTransactionsRepository
          .updateSubmissionStatus(
            submission,
            deletionResult.newSubmissionStatus
          )
      _ = logger.info(
        s"Status for transaction [${submission.transactionId}] updated to ${deletionResult.newSubmissionStatus}"
      )
    } yield for {
      transactionWithDetails <- deletionResult.transactionE
      _ <- dbUpdateE
    } yield transactionWithDetails
  }

  private def handleTransactionDeletion(
      submission: AtalaObjectTransactionSubmission,
      in: Either[CardanoWalletError, Unit]
  ): TransactionDeletionResult =
    in match {
      case Left(
            err @ CardanoWalletError(
              _,
              CardanoWalletErrorCode.TransactionAlreadyInLedger
            )
          ) =>
        TransactionDeletionResult(
          AtalaObjectTransactionSubmissionStatus.InLedger,
          NodeError
            .InternalCardanoWalletError(err)
            .asLeft[AtalaObjectTransactionSubmission]
        )
      case Left(err) =>
        logger.error(
          s"Could not delete transaction ${submission.transactionId}",
          err
        )
        TransactionDeletionResult(
          submission.status,
          NodeError.InternalCardanoWalletError(err).asLeft
        )
      case Right(_) =>
        TransactionDeletionResult(
          AtalaObjectTransactionSubmissionStatus.Deleted,
          submission.asRight
        )
    }

  case class TransactionDeletionResult(
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus,
      transactionE: Either[NodeError, AtalaObjectTransactionSubmission]
  )

  private def mergeAtalaObjects(
      atalaObjects: List[AtalaObjectInfo]
  ): F[List[AtalaObjectInfo]] = {
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

    val objects = atalaObjectsMerged.traverse { case (atalaObject, oldObjects) =>
      if (oldObjects.size != 1) {
        val changedBlockE = atalaObject.getAndValidateAtalaObject
          .flatMap(_.blockContent)
          .toRight {
            NodeError.InternalError(
              s"Block in object ${atalaObject.objectId} was invalidated after merge."
            )
          }

        val atalaObjectIOEither = for {
          changedBlock <- EitherT.fromEither(changedBlockE)
          _ <- EitherT(
            atalaOperationsRepository.updateMergedObjects(
              atalaObject,
              changedBlock.operations.toList,
              oldObjects
            )
          )
        } yield atalaObject

        atalaObjectIOEither.value.map {
          case Left(err) =>
            logger.error(err.toString)
            None
          case Right(atalaObjectInfo) =>
            Some(atalaObjectInfo)
        }
      } else {
        atalaObject.some.pure[F]
      }
    }
    objects.map(_.flatten)
  }

  private def parseObjectContent(
      atalaObjectInfo: AtalaObjectInfo
  ): node_internal.AtalaObject =
    atalaObjectInfo.getAndValidateAtalaObject.getOrElse {
      throw new RuntimeException(
        s"Can't extract AtalaObject content for objectId=${atalaObjectInfo.objectId}"
      )
    }

  private def publishAndRecordTransaction(
      atalaObjectInfo: AtalaObjectInfo,
      atalaObject: node_internal.AtalaObject
  ): F[Either[NodeError, TransactionInfo]] = {
    logger.info(s"Publish atala object [${atalaObjectInfo.objectId}]")
    val publicationEitherT = for {
      // Publish object to the blockchain
      publication <- EitherT(
        ex.deferFuture(atalaReferenceLedger.publish(atalaObject))
      ).leftMap(NodeError.InternalCardanoWalletError)

      _ <- EitherT(
        atalaObjectsTransactionsRepository
          .storeTransactionSubmission(atalaObjectInfo, publication)
      )
    } yield publication.transaction

    publicationEitherT.value
  }

  private def getTransactionDetails(
      transaction: AtalaObjectTransactionSubmission
  ): F[Option[(AtalaObjectTransactionSubmission, TransactionStatus)]] = {
    logger.info(
      s"Getting transaction details for transaction ${transaction.transactionId}"
    )
    for {
      transactionDetails <- ex.deferFuture(
        atalaReferenceLedger.getTransactionDetails(transaction.transactionId)
      )
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
  ): F[Int] = {
    transactions
      .traverse { transaction =>
        atalaObjectsTransactionsRepository
          .updateSubmissionStatus(
            transaction,
            AtalaObjectTransactionSubmissionStatus.InLedger
          )
          .map { dbResultEither =>
            dbResultEither.left.map { err =>
              logger.error(
                s"Could not update status to InLedger for transaction ${transaction.transactionId}",
                err
              )
            }.toOption
          }
      }
      .map(_.flatten.size)
  }
}
