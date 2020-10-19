package io.iohk.atala.prism.node.repositories.daos

import java.time.{Duration, Instant}

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.{AtalaObjectTransactionSubmission, AtalaObjectTransactionSubmissionStatus}
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class AtalaObjectTransactionSubmissionsDAOSpec extends PostgresRepositorySpec {
  private val ONE_SECOND = Duration.ofSeconds(1)

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

  private val atalaObjectId = SHA256Digest.compute("object".getBytes)
  private val byteContent = "byteContent".getBytes
  private val ledger = Ledger.InMemory
  private val transactionId1 = TransactionId.from(SHA256Digest.compute("transactionId1".getBytes).value).value
  private val transactionId2 = TransactionId.from(SHA256Digest.compute("transactionId2".getBytes).value).value
  private val submissionTimestamp = Instant.now

  "AtalaObjectTransactionSubmissionsDAO.insert" should {
    "insert a submission" in {
      insertAtalaObject(atalaObjectId, byteContent)
      val status = AtalaObjectTransactionSubmissionStatus.Pending
      val submission =
        AtalaObjectTransactionSubmission(atalaObjectId, ledger, transactionId1, submissionTimestamp, status)

      AtalaObjectTransactionSubmissionsDAO.insert(submission).runSync

      val retrieved = AtalaObjectTransactionSubmissionsDAO.getBy(submissionTimestamp.plus(ONE_SECOND), status).runSync
      retrieved mustBe List(submission)
    }

    "fail to insert a submission without an existing object" in {
      val submission = AtalaObjectTransactionSubmission(
        atalaObjectId,
        ledger,
        transactionId1,
        submissionTimestamp,
        AtalaObjectTransactionSubmissionStatus.Pending
      )

      assertThrows[Exception] {
        AtalaObjectTransactionSubmissionsDAO.insert(submission).runSync
      }
    }
  }

  "AtalaObjectTransactionSubmissionsDAO.getBy" should {
    "filter by submission timestamp" in {
      val status = AtalaObjectTransactionSubmissionStatus.Pending
      def getByTime(submissionTimestamp: Instant): List[AtalaObjectTransactionSubmission] = {
        AtalaObjectTransactionSubmissionsDAO.getBy(submissionTimestamp, status).runSync
      }
      insertAtalaObject(atalaObjectId, byteContent)
      val submission = AtalaObjectTransactionSubmission(
        atalaObjectId,
        ledger,
        transactionId1,
        submissionTimestamp,
        status
      )
      AtalaObjectTransactionSubmissionsDAO.insert(submission).runSync

      // Older time
      getByTime(submissionTimestamp.minus(ONE_SECOND)) mustBe empty
      // Same time
      getByTime(submissionTimestamp) mustBe empty
      // Later time
      getByTime(submissionTimestamp.plus(ONE_SECOND)) mustBe List(submission)
    }

    "filter by status" in {
      def getByStatus(status: AtalaObjectTransactionSubmissionStatus): List[AtalaObjectTransactionSubmission] = {
        AtalaObjectTransactionSubmissionsDAO.getBy(submissionTimestamp.plus(ONE_SECOND), status).runSync
      }

      insertAtalaObject(atalaObjectId, byteContent)
      val pendingSubmission = AtalaObjectTransactionSubmission(
        atalaObjectId,
        ledger,
        transactionId1,
        submissionTimestamp,
        AtalaObjectTransactionSubmissionStatus.Pending
      )
      val inLedgerSubmission = AtalaObjectTransactionSubmission(
        atalaObjectId,
        ledger,
        transactionId2,
        submissionTimestamp,
        AtalaObjectTransactionSubmissionStatus.InLedger
      )
      AtalaObjectTransactionSubmissionsDAO.insert(pendingSubmission).runSync
      AtalaObjectTransactionSubmissionsDAO.insert(inLedgerSubmission).runSync

      getByStatus(pendingSubmission.status) mustBe List(pendingSubmission)
      getByStatus(inLedgerSubmission.status) mustBe List(inLedgerSubmission)
      getByStatus(AtalaObjectTransactionSubmissionStatus.Deleted) mustBe empty
    }
  }

  "updateStatus" should {
    "update the status" in {
      def getByStatus(status: AtalaObjectTransactionSubmissionStatus): List[AtalaObjectTransactionSubmission] = {
        AtalaObjectTransactionSubmissionsDAO.getBy(submissionTimestamp.plus(ONE_SECOND), status).runSync
      }

      insertAtalaObject(atalaObjectId, byteContent)
      val pendingSubmission = AtalaObjectTransactionSubmission(
        atalaObjectId,
        ledger,
        transactionId1,
        submissionTimestamp,
        AtalaObjectTransactionSubmissionStatus.Pending
      )
      AtalaObjectTransactionSubmissionsDAO.insert(pendingSubmission).runSync

      AtalaObjectTransactionSubmissionsDAO
        .updateStatus(atalaObjectId, AtalaObjectTransactionSubmissionStatus.InLedger)
        .runSync

      getByStatus(AtalaObjectTransactionSubmissionStatus.InLedger) mustBe List(
        pendingSubmission.copy(status = AtalaObjectTransactionSubmissionStatus.InLedger)
      )
    }
  }

  private def insertAtalaObject(objectId: SHA256Digest, byteContent: Array[Byte]): Unit = {
    AtalaObjectsDAO
      .insert(AtalaObjectsDAO.AtalaObjectCreateData(objectId, byteContent))
      .transact(database)
      .unsafeRunSync()
  }

  private implicit class TestOps[T](connection: ConnectionIO[T]) {
    def runSync: T = {
      connection.transact(database).unsafeRunSync()
    }
  }
}
