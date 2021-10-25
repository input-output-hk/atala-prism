package io.iohk.atala.prism.node.repositories

import cats.{Comonad, Functor}
import cats.effect.BracketThrow
import cats.implicits._
import derevo.derive
import derevo.tagless.applyK
import doobie.free.connection
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionStatus}
import io.iohk.atala.prism.node.PublicationInfo
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectInfo,
  AtalaObjectStatus,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus
}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.{AtalaObjectCreateData, AtalaObjectSetTransactionInfo}
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectTransactionSubmissionsDAO, AtalaObjectsDAO}
import io.iohk.atala.prism.node.repositories.logs.AtalaObjectsTransactionsRepositoryLogs
import io.iohk.atala.prism.node.repositories.metrics.AtalaObjectsTransactionsRepositoryMetrics
import io.iohk.atala.prism.node.repositories.utils.connectionIOSafe
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

import java.time.{Duration, Instant}

private class AtalaObjectCannotBeModified extends Exception

@derive(applyK)
trait AtalaObjectsTransactionsRepository[F[_]] {
  def getOldPendingTransactions(
      ledgerPendingTransactionTimeout: Duration,
      ledger: Ledger
  ): F[List[AtalaObjectTransactionSubmission]]

  def retrieveObjects(
      transactions: List[AtalaObjectTransactionSubmission]
  ): F[List[Option[AtalaObjectInfo]]]

  def getNotPublishedObjects: F[Either[NodeError, List[AtalaObjectInfo]]]

  def updateSubmissionStatus(
      submission: AtalaObjectTransactionSubmission,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): F[Either[NodeError, Unit]]

  def updateSubmissionStatusIfExists(
      ledger: Ledger,
      transactionId: TransactionId,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): F[Either[NodeError, Unit]]

  def storeTransactionSubmission(
      atalaObjectInfo: AtalaObjectInfo,
      publication: PublicationInfo
  ): F[Either[NodeError, AtalaObjectTransactionSubmission]]

  def setObjectTransactionDetails(
      notification: AtalaObjectNotification
  ): F[Option[AtalaObjectInfo]]
}

object AtalaObjectsTransactionsRepository {
  def apply[F[_]: BracketThrow: TimeMeasureMetric, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[AtalaObjectsTransactionsRepository[F]] =
    for {
      serviceLogs <- logs.service[AtalaObjectsTransactionsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, AtalaObjectsTransactionsRepository[F]] =
        serviceLogs
      val metrics: AtalaObjectsTransactionsRepository[Mid[F, *]] =
        new AtalaObjectsTransactionsRepositoryMetrics[F]()
      val logs: AtalaObjectsTransactionsRepository[Mid[F, *]] =
        new AtalaObjectsTransactionsRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new AtalaObjectsTransactionsRepositoryImpl[F](transactor)
    }

  def unsafe[F[_]: BracketThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): AtalaObjectsTransactionsRepository[F] =
    AtalaObjectsTransactionsRepository(transactor, logs).extract
}

private final class AtalaObjectsTransactionsRepositoryImpl[F[_]: BracketThrow](
    xa: Transactor[F]
) extends AtalaObjectsTransactionsRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def retrieveObjects(
      transactions: List[AtalaObjectTransactionSubmission]
  ): F[List[Option[AtalaObjectInfo]]] =
    transactions.traverse { transaction =>
      val query = AtalaObjectsDAO.get(transaction.atalaObjectId)

      val opDescription =
        s"Getting atala object by atalaObjectId = ${transaction.atalaObjectId}"
      connectionIOSafe(query.logSQLErrors(opDescription, logger))
        .map {
          case Left(err) =>
            logger.error(
              s"Could not retrieve atala object ${transaction.atalaObjectId}",
              err
            )
            None
          case Right(None) =>
            logger.error(s"Atala object ${transaction.atalaObjectId} not found")
            None
          case Right(result) =>
            result
        }
        .transact(xa)
    }

  def getOldPendingTransactions(
      ledgerPendingTransactionTimeout: Duration,
      ledger: Ledger
  ): F[List[AtalaObjectTransactionSubmission]] = {
    val olderThan = Instant.now.minus(ledgerPendingTransactionTimeout)
    val query = AtalaObjectTransactionSubmissionsDAO
      .getBy(
        olderThan = olderThan,
        status = AtalaObjectTransactionSubmissionStatus.Pending,
        ledger = ledger
      )
      .logSQLErrors("retry old pending transactions", logger)

    connectionIOSafe(query)
      .map(
        _.left
          .map { err =>
            logger.error(
              s"Could not get pending transactions older than $olderThan.",
              err
            )
          }
          .getOrElse(List())
      )
      .transact(xa)
  }

  def getNotPublishedObjects: F[Either[NodeError, List[AtalaObjectInfo]]] = {
    val opDescription = "Extract not submitted objects."
    connectionIOSafe(
      AtalaObjectsDAO.getNotPublishedObjectInfos.logSQLErrors(
        opDescription,
        logger
      )
    ).transact(xa)
  }

  def updateSubmissionStatus(
      submission: AtalaObjectTransactionSubmission,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): F[Either[NodeError, Unit]] =
    if (newSubmissionStatus != submission.status) {
      val query = AtalaObjectTransactionSubmissionsDAO
        .updateStatus(
          submission.ledger,
          submission.transactionId,
          newSubmissionStatus
        )
      val opDescription =
        s"Setting status $newSubmissionStatus for transaction ${submission.transactionId}"
      connectionIOSafe(query.logSQLErrors(opDescription, logger).void)
        .transact(xa)
    } else {
      logger.warn(
        s"Current status of transaction submission [${submission.transactionId}] is already $newSubmissionStatus. Skipping"
      )
      ().asRight[NodeError].pure[F]
    }

  def updateSubmissionStatusIfExists(
      ledger: Ledger,
      transactionId: TransactionId,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): F[Either[NodeError, Unit]] = {
    val query = AtalaObjectTransactionSubmissionsDAO
      .updateStatusIfTxExists(ledger, transactionId, newSubmissionStatus)
    val opDescription =
      s"Setting status $newSubmissionStatus for transaction ${transactionId}"
    connectionIOSafe(query.logSQLErrors(opDescription, logger).void)
      .transact(xa)
  }

  def storeTransactionSubmission(
      atalaObjectInfo: AtalaObjectInfo,
      publication: PublicationInfo
  ): F[Either[NodeError, AtalaObjectTransactionSubmission]] = {
    val opDescription =
      s"publishing and record transaction for [${atalaObjectInfo.objectId}]"
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
      .logSQLErrors(opDescription, logger)

    connectionIOSafe(query).transact(xa)
  }

  def setObjectTransactionDetails(
      notification: AtalaObjectNotification
  ): F[Option[AtalaObjectInfo]] = {
    val objectBytes = notification.atalaObject.toByteArray
    val objId = AtalaObjectId.of(objectBytes)

    val query = for {
      existingObject <- AtalaObjectsDAO.get(objId)
      _ <- {
        existingObject match {
          // Object previously found in the blockchain
          case Some(obj) if obj.transaction.isDefined =>
            connection.raiseError(new AtalaObjectCannotBeModified)
          // Object previously saved in DB, but not in the blockchain
          case Some(_) => connection.unit
          // Object was not in DB, save it to populate transaction data below
          case None =>
            AtalaObjectsDAO.insert(
              AtalaObjectCreateData(
                objId,
                objectBytes,
                AtalaObjectStatus.Processed
              )
            )
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
      .recover { case _: AtalaObjectCannotBeModified =>
        None
      }
  }

  private def toAtalaObjectTransactionSubmissionStatus(
      status: TransactionStatus
  ): AtalaObjectTransactionSubmissionStatus = {
    status match {
      case TransactionStatus.InLedger =>
        AtalaObjectTransactionSubmissionStatus.InLedger
      case TransactionStatus.Submitted =>
        AtalaObjectTransactionSubmissionStatus.Pending
      case TransactionStatus.Expired =>
        AtalaObjectTransactionSubmissionStatus.Pending
      case TransactionStatus.Pending =>
        AtalaObjectTransactionSubmissionStatus.Pending
    }
  }
}
