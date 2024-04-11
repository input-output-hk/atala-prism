package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.fragment.Fragment
import cats.syntax.functor._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.node.models._
import java.time.Instant

object AtalaObjectTransactionSubmissionsDAO {
  private def updateStatusSql(
      ledger: Ledger,
      transactionId: TransactionId,
      status: AtalaObjectTransactionSubmissionStatus
  ): Fragment =
    fr"""
         |UPDATE atala_object_tx_submissions
         |  SET status = $status
         |  WHERE ledger = $ledger
         |    AND transaction_id = $transactionId
       """.stripMargin

  def insert(
      submission: AtalaObjectTransactionSubmission
  ): ConnectionIO[AtalaObjectTransactionSubmission] = {
    sql"""
         |INSERT INTO atala_object_tx_submissions
         |    (atala_object_id, ledger, transaction_id, submission_timestamp, status)
         |VALUES (${submission.atalaObjectId}, ${submission.ledger}, ${submission.transactionId},
         |        ${submission.submissionTimestamp}, ${submission.status})
         |RETURNING atala_object_id, ledger, transaction_id, submission_timestamp, status
       """.stripMargin.query[AtalaObjectTransactionSubmission].unique
  }

  def getLatest(
      ledger: Ledger,
      transactionId: TransactionId
  ): ConnectionIO[Option[AtalaObjectTransactionSubmission]] = {
    sql"""
         |SELECT atala_object_id, ledger, transaction_id, submission_timestamp, status
         |FROM atala_object_tx_submissions
         |WHERE atala_object_id = (
         |  SELECT atala_object_id
         |    FROM atala_object_tx_submissions
         |    WHERE ledger = $ledger
         |      AND transaction_id = $transactionId)
         |  AND ledger = $ledger
         |ORDER BY submission_timestamp DESC
         |LIMIT 1
       """.stripMargin.query[AtalaObjectTransactionSubmission].option
  }

  def getBy(
      olderThan: Instant,
      status: AtalaObjectTransactionSubmissionStatus,
      ledger: Ledger
  ): ConnectionIO[List[AtalaObjectTransactionSubmission]] = {
    sql"""
         |SELECT atala_object_id, ledger, transaction_id, submission_timestamp, status
         |FROM atala_object_tx_submissions
         |WHERE submission_timestamp < $olderThan AND status = $status AND ledger = $ledger
         |ORDER BY submission_timestamp ASC
       """.stripMargin.query[AtalaObjectTransactionSubmission].to[List]
  }

  def updateStatus(
      ledger: Ledger,
      transactionId: TransactionId,
      status: AtalaObjectTransactionSubmissionStatus
  ): ConnectionIO[AtalaObjectTransactionSubmission] = {
    val fragment =
      updateStatusSql(ledger, transactionId, status) ++
        fr"RETURNING atala_object_id, ledger, transaction_id, submission_timestamp, status"
    fragment.query[AtalaObjectTransactionSubmission].unique
  }

  def updateStatusIfTxExists(
      ledger: Ledger,
      transactionId: TransactionId,
      status: AtalaObjectTransactionSubmissionStatus
  ): ConnectionIO[Unit] =
    updateStatusSql(ledger, transactionId, status).update.run.void

  // Returning all transactions in ledger which correspond to an AtalaObject with the status Pending
  // Return sorted by submission_timestamp in descending order
  def getUnconfirmedTransactions(
      lastSeenTransactionId: Option[TransactionId],
      limit: Option[Int]
  ): ConnectionIO[List[TransactionInfo]] = {
    val limitFr = limit.fold(Fragment.empty)(l => fr"LIMIT $l")
    val baseFr: Fragment = lastSeenTransactionId match {
      case None =>
        sql"""
             |SELECT tx.transaction_id, tx.ledger
             |FROM atala_object_tx_submissions AS tx
             |  INNER JOIN atala_objects AS obj ON tx.atala_object_id = obj.atala_object_id
             |WHERE (tx.status = 'PENDING' OR tx.status = 'IN_LEDGER') AND obj.atala_object_status = 'PENDING'
             |ORDER BY tx.submission_timestamp DESC
       """.stripMargin
      case Some(lastSeenId) =>
        sql"""
             |WITH CTE AS (
             |  SELECT submission_timestamp AS last_seen_time
             |  FROM atala_object_tx_submissions
             |  WHERE transaction_id = $lastSeenId
             |)
             |SELECT tx.transaction_id, tx.ledger
             |FROM atala_object_tx_submissions AS tx
             |  INNER JOIN atala_objects AS obj ON tx.atala_object_id = obj.atala_object_id
             |  CROSS JOIN CTE
             |WHERE (tx.status = 'PENDING' OR tx.status = 'IN_LEDGER') AND obj.atala_object_status = 'PENDING' AND
             |       (tx.submission_timestamp < last_seen_time OR
                        tx.submission_timestamp = last_seen_time AND tx.transaction_id < $lastSeenId)
             |ORDER BY tx.submission_timestamp DESC, tx.transaction_id DESC
         """.stripMargin
    }
    (baseFr ++ limitFr)
      .query[TransactionInfo](transactionInfoRead2Columns)
      .to[List]
  }
}
