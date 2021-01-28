package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus
}
import io.iohk.atala.prism.protos.node_internal
import org.scalatest.OptionValues._

import java.time.{Duration, Instant}

class AtalaObjectTransactionSubmissionsDAOSpec extends AtalaWithPostgresSpec {
  private val ONE_SECOND = Duration.ofSeconds(1)

  private val atalaObjectId = AtalaObjectId.of(node_internal.AtalaObject())
  private val atalaObjectId2 = AtalaObjectId.of(node_internal.AtalaObject(blockOperationCount = 2))
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

      val retrieved =
        AtalaObjectTransactionSubmissionsDAO.getBy(submissionTimestamp.plus(ONE_SECOND), status, ledger).runSync
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

  "AtalaObjectTransactionSubmissionsDAO.getLatest" should {
    "return the latest submission of a transaction ID and ledger pair" in {
      def insertSubmission(
          status: AtalaObjectTransactionSubmissionStatus,
          transactionId: TransactionId,
          timestamp: Instant
      ): AtalaObjectTransactionSubmission = {
        AtalaObjectTransactionSubmissionsDAO
          .insert(AtalaObjectTransactionSubmission(atalaObjectId, ledger, transactionId, timestamp, status))
          .runSync
      }
      insertAtalaObject(atalaObjectId, byteContent)
      val start = Instant.now
      insertSubmission(AtalaObjectTransactionSubmissionStatus.Deleted, transactionId1, start)
      val submission2 = insertSubmission(
        AtalaObjectTransactionSubmissionStatus.InLedger,
        transactionId2,
        start.plus(Duration.ofSeconds(2))
      )

      val latest1 = AtalaObjectTransactionSubmissionsDAO.getLatest(ledger, transactionId1).runSync.value
      val latest2 = AtalaObjectTransactionSubmissionsDAO.getLatest(ledger, transactionId2).runSync.value

      // Both transaction IDs should return the same latest submission
      latest1 mustBe submission2
      latest2 mustBe submission2
    }

    "filter out other object IDs" in {
      def insertSubmission(
          atalaObjectId: AtalaObjectId,
          transactionId: TransactionId
      ): AtalaObjectTransactionSubmission = {
        AtalaObjectTransactionSubmissionsDAO
          .insert(
            AtalaObjectTransactionSubmission(
              atalaObjectId,
              ledger,
              transactionId,
              Instant.now,
              AtalaObjectTransactionSubmissionStatus.Pending
            )
          )
          .runSync
      }
      insertAtalaObject(atalaObjectId, byteContent)
      insertAtalaObject(atalaObjectId2, byteContent)
      val submission1 = insertSubmission(atalaObjectId, transactionId1)
      val submission2 = insertSubmission(atalaObjectId2, transactionId2)

      val latest1 = AtalaObjectTransactionSubmissionsDAO.getLatest(ledger, transactionId1).runSync.value
      val latest2 = AtalaObjectTransactionSubmissionsDAO.getLatest(ledger, transactionId2).runSync.value

      latest1 mustBe submission1
      latest2 mustBe submission2
    }

    "filter out other ledgers" in {
      def insertSubmission(
          ledger: Ledger,
          transactionId: TransactionId
      ): AtalaObjectTransactionSubmission = {
        AtalaObjectTransactionSubmissionsDAO
          .insert(
            AtalaObjectTransactionSubmission(
              atalaObjectId,
              ledger,
              transactionId,
              Instant.now,
              AtalaObjectTransactionSubmissionStatus.Pending
            )
          )
          .runSync
      }
      insertAtalaObject(atalaObjectId, byteContent)
      val inMemorySubmission = insertSubmission(Ledger.InMemory, transactionId1)
      val cardanoSubmission = insertSubmission(Ledger.CardanoTestnet, transactionId2)

      val inMemoryLatest = AtalaObjectTransactionSubmissionsDAO.getLatest(Ledger.InMemory, transactionId1).runSync.value
      val cardanoLatest =
        AtalaObjectTransactionSubmissionsDAO.getLatest(Ledger.CardanoTestnet, transactionId2).runSync.value

      inMemoryLatest mustBe inMemorySubmission
      cardanoLatest mustBe cardanoSubmission
    }

    "return None when not found" in {
      val latest = AtalaObjectTransactionSubmissionsDAO.getLatest(ledger, transactionId1).runSync

      latest mustBe None
    }
  }

  "AtalaObjectTransactionSubmissionsDAO.getBy" should {
    "filter by submission timestamp" in {
      val status = AtalaObjectTransactionSubmissionStatus.Pending
      def getByTime(submissionTimestamp: Instant): List[AtalaObjectTransactionSubmission] = {
        AtalaObjectTransactionSubmissionsDAO.getBy(submissionTimestamp, status, ledger).runSync
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
        AtalaObjectTransactionSubmissionsDAO.getBy(submissionTimestamp.plus(ONE_SECOND), status, ledger).runSync
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

    "filter by ledger" in {
      val status = AtalaObjectTransactionSubmissionStatus.Pending
      def getByLedger(ledger: Ledger): List[AtalaObjectTransactionSubmission] = {
        AtalaObjectTransactionSubmissionsDAO.getBy(submissionTimestamp.plus(ONE_SECOND), status, ledger).runSync
      }

      insertAtalaObject(atalaObjectId, byteContent)
      val inMemorySubmission = AtalaObjectTransactionSubmission(
        atalaObjectId,
        Ledger.InMemory,
        transactionId1,
        submissionTimestamp,
        status
      )
      val cardanoTestnetSubmission = AtalaObjectTransactionSubmission(
        atalaObjectId,
        Ledger.CardanoTestnet,
        transactionId2,
        submissionTimestamp,
        status
      )
      AtalaObjectTransactionSubmissionsDAO.insert(inMemorySubmission).runSync
      AtalaObjectTransactionSubmissionsDAO.insert(cardanoTestnetSubmission).runSync

      getByLedger(Ledger.InMemory) mustBe List(inMemorySubmission)
      getByLedger(Ledger.CardanoTestnet) mustBe List(cardanoTestnetSubmission)
      getByLedger(Ledger.BitcoinTestnet) mustBe empty
    }
  }

  "updateStatus" should {
    def getAll(): IndexedSeq[AtalaObjectTransactionSubmission] = {
      AtalaObjectTransactionSubmissionStatus.values
        .flatten { status =>
          AtalaObjectTransactionSubmissionsDAO.getBy(submissionTimestamp.plus(ONE_SECOND), status, ledger).runSync
        }
        .sortBy(_.submissionTimestamp)
    }

    "update the status" in {
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
        .updateStatus(ledger, transactionId1, AtalaObjectTransactionSubmissionStatus.InLedger)
        .runSync

      getAll() mustBe List(pendingSubmission.copy(status = AtalaObjectTransactionSubmissionStatus.InLedger))
    }
  }

  private def insertAtalaObject(objectId: AtalaObjectId, byteContent: Array[Byte]): Unit = {
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
