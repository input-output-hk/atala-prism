package io.iohk.atala.prism.node.repositories

import cats.Applicative
import cats.effect.BracketThrow
import cats.implicits._
import derevo.derive
import derevo.tagless.applyK
import doobie.free.connection
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.models.{Ledger, TransactionStatus}
import io.iohk.atala.prism.node.PublicationInfo
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectInfo,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus
}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.{AtalaObjectCreateData, AtalaObjectSetTransactionInfo}
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectTransactionSubmissionsDAO, AtalaObjectsDAO}
import io.iohk.atala.prism.node.repositories.utils.connectionIOSafe
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

import java.time.{Duration, Instant}

private class AtalaObjectCannotBeModified extends Exception

@derive(applyK)
trait AtalaObjectsTransactionsRepository[F[_]] {
  def getOldPendingTransactions(
      ledgerPendingTransactionTimeout: Duration,
      ledger: Ledger
  ): F[List[AtalaObjectTransactionSubmission]]

  def retrieveObjects(transactions: List[AtalaObjectTransactionSubmission]): F[List[Option[AtalaObjectInfo]]]

  def getNotPublishedObjects: F[Either[NodeError, List[AtalaObjectInfo]]]

  def updateSubmissionStatus(
      submission: AtalaObjectTransactionSubmission,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): F[Either[NodeError, Unit]]

  def storeTransactionSubmission(
      atalaObjectInfo: AtalaObjectInfo,
      publication: PublicationInfo
  ): F[Either[NodeError, AtalaObjectTransactionSubmission]]

  def setObjectTransactionDetails(notification: AtalaObjectNotification): F[Option[AtalaObjectInfo]]
}

object AtalaObjectsTransactionsRepository {
  def apply[F[_]: TimeMeasureMetric: BracketThrow: Applicative](
      transactor: Transactor[F]
  ): AtalaObjectsTransactionsRepository[F] = {
    val metrics: AtalaObjectsTransactionsRepository[Mid[F, *]] = new AtalaObjectsTransactionsRepositoryMetrics[F]()
    metrics attach new AtalaObjectsTransactionsRepositoryImpl[F](transactor)
  }
}

private final class AtalaObjectsTransactionsRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends AtalaObjectsTransactionsRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def retrieveObjects(transactions: List[AtalaObjectTransactionSubmission]): F[List[Option[AtalaObjectInfo]]] =
    transactions.traverse { transaction =>
      val query = AtalaObjectsDAO.get(transaction.atalaObjectId)

      val opDescription = s"Getting atala object by atalaObjectId = ${transaction.atalaObjectId}"
      connectionIOSafe(query.logSQLErrors(opDescription, logger))
        .map {
          case Left(err) =>
            logger.error(s"Could not retrieve atala object ${transaction.atalaObjectId}", err)
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
            logger.error(s"Could not get pending transactions older than $olderThan.", err)
          }
          .getOrElse(List())
      )
      .transact(xa)
  }

  def getNotPublishedObjects: F[Either[NodeError, List[AtalaObjectInfo]]] = {
    val opDescription = "Extract not submitted objects."
    connectionIOSafe(AtalaObjectsDAO.getNotPublishedObjectInfos.logSQLErrors(opDescription, logger)).transact(xa)
  }

  def updateSubmissionStatus(
      submission: AtalaObjectTransactionSubmission,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): F[Either[NodeError, Unit]] =
    if (newSubmissionStatus != submission.status) {
      val query = AtalaObjectTransactionSubmissionsDAO
        .updateStatus(submission.ledger, submission.transactionId, newSubmissionStatus)
      val opDescription = s"Setting status $newSubmissionStatus for transaction ${submission.transactionId}"
      connectionIOSafe(query.logSQLErrors(opDescription, logger).void).transact(xa)
    } else {
      logger.warn(
        s"Current status of transaction submission [${submission.transactionId}] is already $newSubmissionStatus. Skipping"
      )
      ().asRight[NodeError].pure[F]
    }

  def storeTransactionSubmission(
      atalaObjectInfo: AtalaObjectInfo,
      publication: PublicationInfo
  ): F[Either[NodeError, AtalaObjectTransactionSubmission]] = {
    val opDescription = s"publishing and record transaction for [${atalaObjectInfo.objectId}]"
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

  def setObjectTransactionDetails(notification: AtalaObjectNotification): F[Option[AtalaObjectInfo]] = {
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
      .recover {
        case _: AtalaObjectCannotBeModified => None
      }
  }

  private def toAtalaObjectTransactionSubmissionStatus(
      status: TransactionStatus
  ): AtalaObjectTransactionSubmissionStatus = {
    status match {
      case TransactionStatus.InLedger => AtalaObjectTransactionSubmissionStatus.InLedger
      case TransactionStatus.Submitted => AtalaObjectTransactionSubmissionStatus.Pending
      case TransactionStatus.Expired => AtalaObjectTransactionSubmissionStatus.Pending
      case TransactionStatus.Pending => AtalaObjectTransactionSubmissionStatus.Pending
    }
  }
}

private final class AtalaObjectsTransactionsRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends AtalaObjectsTransactionsRepository[Mid[F, *]] {

  private val repoName = "AtalaObjectsTransactionsRepository"

  private lazy val retrieveObjectsTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "retrieveObjects")
  private lazy val getOldPendingTransactionsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getOldPendingTransactions")

  private lazy val getNotPublishedObjectsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getNotPublishedObjects")

  private lazy val updateSubmissionStatusTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "updateSubmissionStatus")

  private lazy val storeTransactionSubmissionTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "storeTransactionSubmission")

  private lazy val setObjectTransactionDetailsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "setObjectTransactionDetails")

  def retrieveObjects(transactions: List[AtalaObjectTransactionSubmission]): Mid[F, List[Option[AtalaObjectInfo]]] =
    _.measureOperationTime(retrieveObjectsTimer)

  def getOldPendingTransactions(
      ledgerPendingTransactionTimeout: Duration,
      ledger: Ledger
  ): Mid[F, List[AtalaObjectTransactionSubmission]] =
    _.measureOperationTime(getOldPendingTransactionsTimer)

  def getNotPublishedObjects: Mid[F, Either[NodeError, List[AtalaObjectInfo]]] =
    _.measureOperationTime(getNotPublishedObjectsTimer)

  def updateSubmissionStatus(
      submission: AtalaObjectTransactionSubmission,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): Mid[F, Either[NodeError, Unit]] =
    _.measureOperationTime(updateSubmissionStatusTimer)

  def storeTransactionSubmission(
      atalaObjectInfo: AtalaObjectInfo,
      publication: PublicationInfo
  ): Mid[F, Either[NodeError, AtalaObjectTransactionSubmission]] =
    _.measureOperationTime(storeTransactionSubmissionTimer)

  def setObjectTransactionDetails(notification: AtalaObjectNotification): Mid[F, Option[AtalaObjectInfo]] =
    _.measureOperationTime(setObjectTransactionDetailsTimer)
}
