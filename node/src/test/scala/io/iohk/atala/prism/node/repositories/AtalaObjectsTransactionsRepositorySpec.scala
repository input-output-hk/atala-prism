package io.iohk.atala.prism.node.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.node.AtalaWithPostgresSpec
import io.iohk.atala.prism.node.models.{BlockInfo, Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.utils.IOUtils._
import org.scalatest.OptionValues.convertOptionToValuable
import tofu.logging.Logs

import java.time.Instant

class AtalaObjectsTransactionsRepositorySpec extends AtalaWithPostgresSpec {
  private val testLogs = Logs.sync[IO, IO]
  private lazy implicit val repository: AtalaObjectsTransactionsRepository[IO] =
    AtalaObjectsTransactionsRepository.unsafe(database, testLogs)

  private val signedOperationDummy =
    BlockProcessingServiceSpec.signedCreateDidOperation
  private val blockDummy = DataPreparation.createBlock(signedOperationDummy)
  private val objectDummy = DataPreparation.createAtalaObject(blockDummy)

  private val atalaObjectNotificationDummy =
    AtalaObjectNotification(objectDummy, DataPreparation.dummyTransactionInfo)

  "AtalaObjectsTransactionsRepository.setObjectTransactionDetails" should {
    "not schedule new operations" in {
      val objectInfo = repository
        .setObjectTransactionDetails(atalaObjectNotificationDummy)
        .unsafeToFuture()
        .futureValue
      objectInfo.isDefined must be(true)

      val notPublishedObjects = repository.getNotPublishedObjects
        .unsafeToFuture()
        .futureValue
        .toOption
        .get
      notPublishedObjects.isEmpty must be(true)
    }
  }

  "AtalaObjectsTransactionsRepository.getConfirmedObjectTransactions" should {
    "retrieve all the confirmed transactions" in {
      val transactionInfos = setObjectTransactionDetails3Objects()

      val resultTxInfos = getConfirmedObjectTransactionsRunner(None, 10)

      resultTxInfos must be(transactionInfos.reverse)
    }

    "retrieve 2 last confirmed transactions" in {
      val transactionInfos = setObjectTransactionDetails3Objects()

      val resultTxInfos = getConfirmedObjectTransactionsRunner(None, 2)

      resultTxInfos must be(transactionInfos.reverse.take(2))
    }

    "retrieve confirmed transactions with last seen txId" in {
      val transactionInfos = setObjectTransactionDetails3Objects()

      val resultTxInfos = getConfirmedObjectTransactionsRunner(Some(transactionInfos.last.transactionId), 10)
      resultTxInfos must be(transactionInfos.reverse.takeRight(2))

      val resultTxInfos2 = getConfirmedObjectTransactionsRunner(Some(transactionInfos(1).transactionId), 10)
      resultTxInfos2 must be(transactionInfos.reverse.takeRight(1))

      val resultTxInfos3 = getConfirmedObjectTransactionsRunner(Some(transactionInfos.last.transactionId), 1)
      resultTxInfos3 must be(transactionInfos.reverse.slice(1, 2))
    }

    "retrieve confirmed transactions with last seen txId (tx in the same blocks)" in {
      val transactionInfos = setObjectTransactionDetails3Objects()

      val resultTxInfos = getConfirmedObjectTransactionsRunner(Some(transactionInfos.last.transactionId), 10)
      resultTxInfos must be(transactionInfos.reverse.takeRight(2))
    }
  }

  def getConfirmedObjectTransactionsRunner(
      lastSeenId: Option[TransactionId] = None,
      limit: Int = 50
  ): List[TransactionInfo] = {
    repository
      .getConfirmedObjectTransactions(lastSeenId, limit)
      .unsafeToFuture()
      .futureValue
      .toOption
      .get
  }

  private def setObjectTransactionDetails3Objects(differentBlocks: Boolean = true): List[TransactionInfo] = {
    val epochMillis: List[Long] = List(10, 20, 30)
    val txIds = List(
      Sha256Hash.compute("id1".getBytes).bytes,
      Sha256Hash.compute("id2".getBytes).bytes,
      Sha256Hash.compute("id3".getBytes).bytes
    ).map(TransactionId.from(_).value)

    val blockInfos =
      if (differentBlocks)
        epochMillis.zipWithIndex map { case (millis, bIdx) =>
          BlockInfo(
            number = bIdx + 1,
            timestamp = Instant.ofEpochMilli(millis),
            index = 1
          )
        }
      else
        List(
          BlockInfo(
            number = 1,
            timestamp = Instant.ofEpochMilli(epochMillis.head),
            index = 1
          ),
          BlockInfo(
            number = 1,
            timestamp = Instant.ofEpochMilli(epochMillis.head),
            index = 2
          ),
          BlockInfo(
            number = 2,
            timestamp = Instant.ofEpochMilli(epochMillis(1)),
            index = 1
          )
        )

    val transactionInfos = blockInfos.zip(txIds) map { case (bInfo, txId) =>
      TransactionInfo(
        transactionId = txId,
        ledger = Ledger.InMemory,
        block = Some(bInfo)
      )
    }

    val blockOperations =
      List(
        List(BlockProcessingServiceSpec.signedCreateDidOperation),
        List(BlockProcessingServiceSpec.signedUpdateDidOperation),
        List(BlockProcessingServiceSpec.signedCreateDidOperation, BlockProcessingServiceSpec.signedUpdateDidOperation)
      )

    val objects = blockOperations.map(ops => DataPreparation.createAtalaObject(DataPreparation.createBlock(ops)))

    objects.zip(transactionInfos).foreach { case (ob, ti) =>
      repository
        .setObjectTransactionDetails(AtalaObjectNotification(ob, ti))
        .unsafeToFuture()
        .futureValue
    }
    transactionInfos
  }
}
