package io.iohk.atala.prism.node.repositories.metrics

import cats.effect.MonadCancelThrow
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.PublicationInfo
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.{
  AtalaObjectInfo,
  AtalaObjectStatus,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus
}
import io.iohk.atala.prism.node.repositories.AtalaObjectsTransactionsRepository
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import tofu.higherKind.Mid

import java.time.Duration

private[repositories] final class AtalaObjectsTransactionsRepositoryMetrics[F[
    _
]: TimeMeasureMetric: MonadCancelThrow]
    extends AtalaObjectsTransactionsRepository[Mid[F, *]] {

  private val repoName = "AtalaObjectsTransactionsRepository"

  private lazy val retrieveObjectsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "retrieveObjects")
  private lazy val getOldPendingTransactionsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getOldPendingTransactions")

  private lazy val getNotPublishedObjectsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getNotPublishedObjects")

  private lazy val getNotProcessedObjectsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getNotProcessedObjects")

  private lazy val getUnconfirmedObjectTransactionsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getUnconfirmedObjectTransactions")

  private lazy val getConfirmedObjectTransactionsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getConfirmedObjectTransactions")

  private lazy val updateSubmissionStatusTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "updateSubmissionStatus")

  private lazy val updateSubmissionStatusIfExistsTimer =
    TimeMeasureUtil.createDBQueryTimer(
      repoName,
      "updateSubmissionStatusIfExists"
    )

  private lazy val updateObjectStatusTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "updateObjectStatus")

  private lazy val storeTransactionSubmissionTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "storeTransactionSubmission")

  private lazy val setObjectTransactionDetailsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "setObjectTransactionDetails")

  def retrieveObjects(
      transactions: List[AtalaObjectTransactionSubmission]
  ): Mid[F, List[Either[NodeError, Option[AtalaObjectInfo]]]] =
    _.measureOperationTime(retrieveObjectsTimer)

  def getOldPendingTransactions(
      ledgerPendingTransactionTimeout: Duration,
      ledger: Ledger
  ): Mid[F, Either[NodeError, List[AtalaObjectTransactionSubmission]]] =
    _.measureOperationTime(getOldPendingTransactionsTimer)

  def getNotPublishedObjects: Mid[F, Either[NodeError, List[AtalaObjectInfo]]] =
    _.measureOperationTime(getNotPublishedObjectsTimer)

  def updateSubmissionStatus(
      submission: AtalaObjectTransactionSubmission,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): Mid[F, Either[NodeError, Unit]] =
    _.measureOperationTime(updateSubmissionStatusTimer)

  override def updateSubmissionStatusIfExists(
      ledger: Ledger,
      transactionId: TransactionId,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): Mid[F, Either[NodeError, Unit]] =
    _.measureOperationTime(updateSubmissionStatusIfExistsTimer)

  def storeTransactionSubmission(
      atalaObjectInfo: AtalaObjectInfo,
      publication: PublicationInfo
  ): Mid[F, Either[NodeError, AtalaObjectTransactionSubmission]] =
    _.measureOperationTime(storeTransactionSubmissionTimer)

  def setObjectTransactionDetails(
      notification: AtalaObjectNotification
  ): Mid[F, Option[AtalaObjectInfo]] =
    _.measureOperationTime(setObjectTransactionDetailsTimer)

  def updateObjectStatus(
      oldObjectStatus: AtalaObjectStatus,
      newObjectStatus: AtalaObjectStatus
  ): Mid[F, Either[NodeError, Int]] =
    _.measureOperationTime(updateObjectStatusTimer)

  override def getNotProcessedObjects: Mid[F, Either[NodeError, List[AtalaObjectInfo]]] =
    _.measureOperationTime(getNotProcessedObjectsTimer)

  override def getUnconfirmedObjectTransactions(
      lastSeenTxId: Option[TransactionId],
      limit: Int
  ): Mid[F, Either[NodeError, List[TransactionInfo]]] =
    _.measureOperationTime(getUnconfirmedObjectTransactionsTimer)

  override def getConfirmedObjectTransactions(
      lastSeenTxId: Option[TransactionId],
      limit: Int
  ): Mid[F, Either[NodeError, List[TransactionInfo]]] =
    _.measureOperationTime(getConfirmedObjectTransactionsTimer)
}
