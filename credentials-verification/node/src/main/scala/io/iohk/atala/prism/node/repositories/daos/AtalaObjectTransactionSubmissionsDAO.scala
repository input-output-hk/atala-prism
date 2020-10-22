package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.DoobieImplicits._
import io.iohk.atala.prism.models.Ledger
import io.iohk.atala.prism.node.models.{AtalaObjectTransactionSubmission, AtalaObjectTransactionSubmissionStatus}

object AtalaObjectTransactionSubmissionsDAO {
  def insert(submission: AtalaObjectTransactionSubmission): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO atala_object_tx_submissions
         |    (atala_object_id, ledger, transaction_id, submission_timestamp, status)
         |VALUES (${submission.atalaObjectId}, ${submission.ledger}, ${submission.transactionId},
         |        ${submission.submissionTimestamp}, ${submission.status})
       """.stripMargin.update.run.map(_ => ())
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
       """.stripMargin.query[AtalaObjectTransactionSubmission].to[List]
  }

  def updateStatus(
      atalaObjectId: SHA256Digest,
      status: AtalaObjectTransactionSubmissionStatus
  ): ConnectionIO[AtalaObjectTransactionSubmission] = {
    sql"""
         |UPDATE atala_object_tx_submissions
         |  SET status = $status
         |  WHERE atala_object_id = $atalaObjectId
         |RETURNING atala_object_id, ledger, transaction_id, submission_timestamp, status
       """.stripMargin.query[AtalaObjectTransactionSubmission].unique
  }
}
