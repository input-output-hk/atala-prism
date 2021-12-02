package io.iohk.atala.prism.node.services

import cats.{Applicative, Comonad, Functor, Monad}
import cats.data.EitherT
import cats.effect.Resource
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
import io.iohk.atala.prism.node.services.models.UpdateTransactionStatusesResult
import io.iohk.atala.prism.protos.node_internal
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}

import java.time.Duration
import cats.MonadThrow

@derive(applyK)
trait SubmissionService[F[_]] {

  /** Returns number of published transactions
    */
  def submitReceivedObjects(): F[Either[NodeError, Int]]

  def updateTransactionStatuses(): F[UpdateTransactionStatusesResult]

}

object SubmissionService {

  case class Config(
      maxNumberTransactionsToSubmit: Int
  )

  def apply[F[_]: MonadThrow, R[_]: Functor](
      atalaReferenceLedger: UnderlyingLedger[F],
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      config: Config = Config(Int.MaxValue),
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

  def resource[F[_]: MonadThrow, R[_]: Applicative: Functor](
      atalaReferenceLedger: UnderlyingLedger[F],
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      config: Config = Config(Int.MaxValue),
      logs: Logs[R, F]
  ): Resource[R, SubmissionService[F]] = Resource.eval(
    SubmissionService(
      atalaReferenceLedger,
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      config,
      logs
    )
  )

  def unsafe[F[_]: MonadThrow, R[_]: Comonad](
      atalaReferenceLedger: UnderlyingLedger[F],
      atalaOperationsRepository: AtalaOperationsRepository[F],
      atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
      config: Config = Config(Int.MaxValue),
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
    atalaReferenceLedger: UnderlyingLedger[F],
    atalaOperationsRepository: AtalaOperationsRepository[F],
    atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[F],
    config: Config
) extends SubmissionService[F] {

  // gets all pending operations from the database and merges them into bigger AtalaObjects
  // then publishes these objects to the ledger as transaction metadata.
  // every AtalaObject corresponds to one transaction metadata
  def submitReceivedObjects(): F[Either[NodeError, Int]] = {
    val submissionET = for {
      // execute SQL query to get all pending objects
      atalaObjects <- EitherT(
        atalaObjectsTransactionsRepository.getNotPublishedObjects
      )
      // merge AtalaObjects into bigger AtalaObjects in order to reduce amount of created transactions
      atalaObjectsMerged <- EitherT.right(mergeAtalaObjects(atalaObjects))
      // deserialize resulting AtalaObjects from bytes
      atalaObjectsWithParsedContent = atalaObjectsMerged.map { obj =>
        (obj, parseObjectContent(obj))
      }
      // publish AtalaObjects to the ledger. every AtalaObject represents one transaction metadata
      publishedTransactions <-
        EitherT.right[NodeError](
          publishObjectsAndRecordTransaction(atalaObjectsWithParsedContent)
        )
    } yield publishedTransactions.size

    submissionET.value
  }

  def updateTransactionStatuses(): F[UpdateTransactionStatusesResult] = {
    val getOldPendingTransactions =
      atalaObjectsTransactionsRepository
        .getOldPendingTransactions(
          Duration.ZERO,
          atalaReferenceLedger.getType
        )
        .map(_.toList.flatten)

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

      expiredTransactions = notInLedgerTransactions.collect {
        case (transaction, status) if status == TransactionStatus.Expired =>
          transaction
      }

      deletedTransactions <- deleteTransactions(expiredTransactions)
    } yield UpdateTransactionStatusesResult(pendingTransactions.size, numInLedgerSynced, deletedTransactions.size)
  }

  private def publishObjectsAndRecordTransaction(
      atalaObjectsWithParsedContent: List[
        (AtalaObjectInfo, node_internal.AtalaObject)
      ]
  ): F[List[TransactionInfo]] = {
    def justKeep(
        keep: List[TransactionInfo]
    ): NodeError => List[TransactionInfo] = { _ =>
      keep
    }

    // take no more than config.maxNumberTransactionsToSubmit objects, so it means that we limit amount of transactions per one submission.
    // then, sequentially publish objects one by one and aggregate results
    atalaObjectsWithParsedContent
      .take(config.maxNumberTransactionsToSubmit)
      .foldLeft(Monad[F].pure(List.empty[TransactionInfo])) { case (accF, (obj, objContent)) =>
        for {
          acc <- accF // accumulated transaction information from previous publications
          transactionInfoE <- publishAndRecordTransaction(obj, objContent) // the next publication info
        } yield {
          transactionInfoE.fold(justKeep(acc), _ :: acc)
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
  ): F[Either[NodeError, AtalaObjectTransactionSubmission]] =
    for {
      deletionResult <-
        atalaReferenceLedger
          .deleteTransaction(submission.transactionId)
          .map(handleTransactionDeletion(submission, _))
      dbUpdateE <-
        atalaObjectsTransactionsRepository
          .updateSubmissionStatus(
            submission,
            deletionResult.newSubmissionStatus
          )
    } yield for {
      transactionWithDetails <- deletionResult.transactionE
      _ <- dbUpdateE
    } yield transactionWithDetails

  private def handleTransactionDeletion(
      submission: AtalaObjectTransactionSubmission,
      in: Either[CardanoWalletError, Unit]
  ): TransactionDeletionResult =
    in match {
      case Left(err @ CardanoWalletError(_, CardanoWalletErrorCode.TransactionAlreadyInLedger)) =>
        TransactionDeletionResult(
          AtalaObjectTransactionSubmissionStatus.InLedger,
          NodeError.InternalCardanoWalletError(err).asLeft[AtalaObjectTransactionSubmission]
        )
      case Left(err) =>
        TransactionDeletionResult(submission.status, NodeError.InternalCardanoWalletError(err).asLeft)
      case Right(_) =>
        TransactionDeletionResult(AtalaObjectTransactionSubmissionStatus.Deleted, submission.asRight)
    }

  case class TransactionDeletionResult(
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus,
      transactionE: Either[NodeError, AtalaObjectTransactionSubmission]
  )

  private def mergeAtalaObjects(
      atalaObjects: List[AtalaObjectInfo]
  ): F[List[AtalaObjectInfo]] = {
    // Iterate over objects and merge when the size of the resulting object fits into transaction metadata
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

    // For every operation we store the corresponding AtalaObject containing this operation.
    // Here we look into merged objects and update the object identifier for the operations inside this object.
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
          case Left(_) => None
          case Right(atalaObjectInfo) => Some(atalaObjectInfo)
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
    val publicationEitherT = for {
      // Publish object to the blockchain
      publication <- EitherT(atalaReferenceLedger.publish(atalaObject))
        .leftMap(NodeError.InternalCardanoWalletError)

      // Store the resulting publication information into the Node's database.
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
    for {
      transactionDetails <- atalaReferenceLedger.getTransactionDetails(
        transaction.transactionId
      )
    } yield {
      transactionDetails.map { transactionDetails =>
        (transaction, transactionDetails.status)
      }.toOption
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
          .map(_.toOption)
      }
      .map(_.flatten.size)
  }
}
