package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import cats.syntax.functor._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.{AtalaObjectTransactionSubmission, AtalaObjectTransactionSubmissionStatus}

object AtalaObjectTransactionSubmissionsDAO {
  def insert(submission: AtalaObjectTransactionSubmission): ConnectionIO[AtalaObjectTransactionSubmission] = {
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
    sql"""
         |UPDATE atala_object_tx_submissions
         |  SET status = $status
         |  WHERE ledger = $ledger
         |    AND transaction_id = $transactionId
         |RETURNING atala_object_id, ledger, transaction_id, submission_timestamp, status
       """.stripMargin.query[AtalaObjectTransactionSubmission].unique
  }

  def updateStatusIfTxExists(
      ledger: Ledger,
      transactionId: TransactionId,
      status: AtalaObjectTransactionSubmissionStatus
  ): ConnectionIO[Unit] = {
    sql"""
         |UPDATE atala_object_tx_submissions
         |  SET status = $status
         |  WHERE ledger = $ledger
         |    AND transaction_id = $transactionId
       """.stripMargin.update.run.void
  }
}
