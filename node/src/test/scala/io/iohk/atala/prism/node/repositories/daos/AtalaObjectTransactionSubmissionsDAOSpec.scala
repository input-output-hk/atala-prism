package io.iohk.atala.prism.node.repositories.daos

import cats.effect.unsafe.implicits.global
import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.node.models.Ledger.InMemory
import io.iohk.atala.prism.node.models.{Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.{AtalaWithPostgresSpec, DataPreparation}
import io.iohk.atala.prism.node.models.AtalaObjectTransactionSubmissionStatus.{InLedger, Pending}
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectStatus,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus
}
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.protos.node_models
import org.scalatest.OptionValues._
import scalapb.UnknownFieldSet

import java.time.{Duration, Instant}

class AtalaObjectTransactionSubmissionsDAOSpec extends AtalaWithPostgresSpec {
  private val ONE_SECOND = Duration.ofSeconds(1)

  private val atalaObjectId = AtalaObjectId.of(node_models.AtalaObject())

  private val atalaObjectId2 =
    AtalaObjectId.of(
      node_models
        .AtalaObject()
        .withUnknownFields(
          // something to differentiate one object from another
          UnknownFieldSet.empty.withField(2, UnknownFieldSet.Field(fixed32 = Seq(2)))
        )
    )
  private val byteContent = "byteContent".getBytes
  private val ledger = Ledger.InMemory
  private val transactionId1 =
    TransactionId.from(Sha256.compute("transactionId1".getBytes).getValue).value
  private val transactionId2 =
    TransactionId.from(Sha256.compute("transactionId2".getBytes).getValue).value
  private val submissionTimestamp = Instant.now

  "AtalaObjectTransactionSubmissionsDAO.insert" should {
    "insert a submission" in {
      insertAtalaObject(atalaObjectId, byteContent)
      val status = AtalaObjectTransactionSubmissionStatus.Pending
      val submission =
        AtalaObjectTransactionSubmission(
          atalaObjectId,
          ledger,
          transactionId1,
          submissionTimestamp,
          status
        )

      AtalaObjectTransactionSubmissionsDAO.insert(submission).runSync

      val retrieved =
        AtalaObjectTransactionSubmissionsDAO
          .getBy(submissionTimestamp.plus(ONE_SECOND), status, ledger)
          .runSync
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
          .insert(
            AtalaObjectTransactionSubmission(
              atalaObjectId,
              ledger,
              transactionId,
              timestamp,
              status
            )
          )
          .runSync
      }
      insertAtalaObject(atalaObjectId, byteContent)
      val start = Instant.now
      insertSubmission(
        AtalaObjectTransactionSubmissionStatus.Deleted,
        transactionId1,
        start
      )
      val submission2 = insertSubmission(
        AtalaObjectTransactionSubmissionStatus.InLedger,
        transactionId2,
        start.plus(Duration.ofSeconds(2))
      )

      val latest1 = AtalaObjectTransactionSubmissionsDAO
        .getLatest(ledger, transactionId1)
        .runSync
        .value
      val latest2 = AtalaObjectTransactionSubmissionsDAO
        .getLatest(ledger, transactionId2)
        .runSync
        .value

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

      val latest1 = AtalaObjectTransactionSubmissionsDAO
        .getLatest(ledger, transactionId1)
        .runSync
        .value
      val latest2 = AtalaObjectTransactionSubmissionsDAO
        .getLatest(ledger, transactionId2)
        .runSync
        .value

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
      val cardanoSubmission =
        insertSubmission(Ledger.CardanoTestnet, transactionId2)

      val inMemoryLatest = AtalaObjectTransactionSubmissionsDAO
        .getLatest(Ledger.InMemory, transactionId1)
        .runSync
        .value
      val cardanoLatest =
        AtalaObjectTransactionSubmissionsDAO
          .getLatest(Ledger.CardanoTestnet, transactionId2)
          .runSync
          .value

      inMemoryLatest mustBe inMemorySubmission
      cardanoLatest mustBe cardanoSubmission
    }

    "return None when not found" in {
      val latest = AtalaObjectTransactionSubmissionsDAO
        .getLatest(ledger, transactionId1)
        .runSync

      latest mustBe None
    }
  }

  "AtalaObjectTransactionSubmissionsDAO.getBy" should {
    "filter by submission timestamp" in {
      val status = AtalaObjectTransactionSubmissionStatus.Pending
      def getByTime(
          submissionTimestamp: Instant
      ): List[AtalaObjectTransactionSubmission] = {
        AtalaObjectTransactionSubmissionsDAO
          .getBy(submissionTimestamp, status, ledger)
          .runSync
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
      def getByStatus(
          status: AtalaObjectTransactionSubmissionStatus
      ): List[AtalaObjectTransactionSubmission] = {
        AtalaObjectTransactionSubmissionsDAO
          .getBy(submissionTimestamp.plus(ONE_SECOND), status, ledger)
          .runSync
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
      def getByLedger(
          ledger: Ledger
      ): List[AtalaObjectTransactionSubmission] = {
        AtalaObjectTransactionSubmissionsDAO
          .getBy(submissionTimestamp.plus(ONE_SECOND), status, ledger)
          .runSync
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
      AtalaObjectTransactionSubmissionsDAO
        .insert(cardanoTestnetSubmission)
        .runSync

      getByLedger(Ledger.InMemory) mustBe List(inMemorySubmission)
      getByLedger(Ledger.CardanoTestnet) mustBe List(cardanoTestnetSubmission)
    }
  }

  "AtalaObjectTransactionSubmissionsDAO.updateStatus" should {
    def getAll(): IndexedSeq[AtalaObjectTransactionSubmission] = {
      AtalaObjectTransactionSubmissionStatus.values
        .flatten { status =>
          AtalaObjectTransactionSubmissionsDAO
            .getBy(submissionTimestamp.plus(ONE_SECOND), status, ledger)
            .runSync
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
        .updateStatus(
          ledger,
          transactionId1,
          AtalaObjectTransactionSubmissionStatus.InLedger
        )
        .runSync

      getAll() mustBe List(
        pendingSubmission.copy(status = AtalaObjectTransactionSubmissionStatus.InLedger)
      )
    }
  }

  "AtalaObjectTransactionSubmissionsDAO.getUnconfirmedTransactions" should {
    "get all unconfirmed transactions" in {
      val objIds = insertAtalaObjects()
      val expectedTxs = insertTransactionSubmissions(objIds).reverse

      val resultTxs =
        AtalaObjectTransactionSubmissionsDAO.getUnconfirmedTransactions(None, None).runSync

      resultTxs must be(expectedTxs)
    }

    "get unconfirmed transactions after lastTxId" in {
      val objIds = insertAtalaObjects()
      val expectedTxs = insertTransactionSubmissions(objIds).reverse

      val resultTxs =
        AtalaObjectTransactionSubmissionsDAO
          .getUnconfirmedTransactions(Some(expectedTxs.head.transactionId), None)
          .runSync
      resultTxs must be(expectedTxs.tail)

      val resultTxs2 =
        AtalaObjectTransactionSubmissionsDAO
          .getUnconfirmedTransactions(Some(expectedTxs.head.transactionId), Some(1))
          .runSync
      resultTxs2 must be(expectedTxs.slice(1, 2))
    }
  }

  private def insertTransactionSubmissions(
      objectIds: List[AtalaObjectId],
      statuses: List[AtalaObjectTransactionSubmissionStatus] = List(Pending, InLedger, InLedger)
  ): List[TransactionInfo] = {
    val txIds = (1 to objectIds.size)
      .map(idx => TransactionId.from(Sha256.compute(s"transactionId${idx + 1}".getBytes).getValue).value)
      .toList

    objectIds.zip(statuses).zipWithIndex.zip(txIds).foreach { case (((objId, st), idx), txId) =>
      val submission = AtalaObjectTransactionSubmission(
        objId,
        ledger,
        txId,
        Instant.ofEpochMilli(10.toLong * (idx + 1)),
        st
      )
      AtalaObjectTransactionSubmissionsDAO.insert(submission).runSync
    }
    txIds.map(id => TransactionInfo(transactionId = id, InMemory))
  }

  private def insertAtalaObjects(
      statuses: List[AtalaObjectStatus] =
        List(AtalaObjectStatus.Pending, AtalaObjectStatus.Pending, AtalaObjectStatus.Pending)
  ): List[AtalaObjectId] = {
    val blockOperations =
      List(
        List(BlockProcessingServiceSpec.signedCreateDidOperation),
        List(BlockProcessingServiceSpec.signedUpdateDidOperation),
        List(BlockProcessingServiceSpec.signedCreateDidOperation, BlockProcessingServiceSpec.signedUpdateDidOperation)
      )
    val atalaObjects = blockOperations.map(ops => DataPreparation.createAtalaObject(DataPreparation.createBlock(ops)))

    atalaObjects.zip(statuses).foreach { case (obj, st) =>
      AtalaObjectsDAO
        .insert(
          AtalaObjectsDAO.AtalaObjectCreateData(AtalaObjectId.of(obj), obj.toByteArray, st)
        )
        .transact(database)
        .unsafeToFuture()
        .void
        .futureValue
    }
    atalaObjects.map(AtalaObjectId.of)
  }

  private def insertAtalaObject(
      objectId: AtalaObjectId,
      byteContent: Array[Byte]
  ): Unit = {
    AtalaObjectsDAO
      .insert(AtalaObjectsDAO.AtalaObjectCreateData(objectId, byteContent, AtalaObjectStatus.Scheduled))
      .transact(database)
      .unsafeToFuture()
      .void
      .futureValue
  }

  private implicit class TestOps[T](connection: ConnectionIO[T]) {
    def runSync: T = {
      connection.transact(database).unsafeRunSync()
    }
  }
}
