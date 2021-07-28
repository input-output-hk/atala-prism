package io.iohk.atala.prism.node.repositories.daos

import cats.syntax.functor._
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{BlockInfo, Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.models.{AtalaObjectInfo, AtalaObjectId}
import io.iohk.atala.prism.protos.node_internal
import org.scalatest.OptionValues._

import java.time.Instant

class AtalaObjectsDAOSpec extends AtalaWithPostgresSpec {
  private val objectId = AtalaObjectId.of(node_internal.AtalaObject())
  private val byteContent = "byteContent".getBytes
  private val transactionInfo = TransactionInfo(
    transactionId = TransactionId.from(SHA256Digest.compute("transactionId".getBytes).value).value,
    ledger = Ledger.InMemory,
    block = Some(BlockInfo(number = 1, timestamp = Instant.ofEpochMilli(133713371337L), index = 0))
  )

  "AtalaObjectsDAO.insert" should {
    "insert an object" in {
      insert(objectId, byteContent)

      val retrieved = get(objectId)
      retrieved.objectId mustBe objectId
      retrieved.byteContent mustBe byteContent
      retrieved.transaction mustBe None
      retrieved.processed mustBe false
    }
  }

  "AtalaObjectsDAO.setTransactionInfo" should {
    "set the transaction info of an existing object" in {
      insert(objectId, byteContent)

      AtalaObjectsDAO
        .setTransactionInfo(AtalaObjectsDAO.AtalaObjectSetTransactionInfo(objectId, transactionInfo))
        .transact(database)
        .unsafeRunSync()

      val retrieved = get(objectId)
      retrieved.objectId mustBe objectId
      retrieved.byteContent mustBe byteContent
      retrieved.transaction.value mustBe transactionInfo
      retrieved.processed mustBe false
    }

    "fail to set the transaction info of a nonexistent object" in {
      assertThrows[Exception] {
        AtalaObjectsDAO
          .setTransactionInfo(AtalaObjectsDAO.AtalaObjectSetTransactionInfo(objectId, transactionInfo))
          .transact(database)
          .unsafeRunSync()
      }
    }

    "fail to set the transaction info without a block" in {
      insert(objectId, byteContent)

      assertThrows[Exception] {
        AtalaObjectsDAO
          .setTransactionInfo(
            AtalaObjectsDAO.AtalaObjectSetTransactionInfo(objectId, transactionInfo.copy(block = None))
          )
          .transact(database)
          .unsafeRunSync()
      }
    }
  }

  "AtalaObjectsDAO.get" should {
    "get an object without transaction info" in {
      insert(objectId, byteContent)

      val retrieved = get(objectId)

      retrieved.objectId mustBe objectId
      retrieved.byteContent mustBe byteContent
      retrieved.transaction mustBe None
      retrieved.processed mustBe false
    }

    "get an object with transaction info" in {
      insert(objectId, byteContent)
      AtalaObjectsDAO
        .setTransactionInfo(AtalaObjectsDAO.AtalaObjectSetTransactionInfo(objectId, transactionInfo))
        .transact(database)
        .unsafeRunSync()

      val retrieved = get(objectId)

      retrieved.objectId mustBe objectId
      retrieved.byteContent mustBe byteContent
      retrieved.transaction.value mustBe transactionInfo
      retrieved.processed mustBe false
    }
  }

  "AtalaObjectsDAO.setProcessed" should {
    "mark object as processed" in {
      insert(objectId, byteContent)

      AtalaObjectsDAO.setProcessed(objectId).transact(database).unsafeRunSync()

      val retrieved = get(objectId)
      retrieved.processed mustBe true
    }
  }

  "AtalaObjectsDAO.getNotPublishedObjectIds" should {
    "return object ids in the correct order" in {
      val N = 10
      (0 until N).foreach { count =>
        val objId = AtalaObjectId.of(node_internal.AtalaObject(blockOperationCount = count))
        insert(objId, byteContent)
      }
      val retrieved = AtalaObjectsDAO.getNotPublishedObjectIds.transact(database).unsafeRunSync()
      retrieved.size mustBe N
      retrieved.zipWithIndex.foreach {
        case (objId, ind) =>
          withClue(s"Index $ind:") {
            objId mustBe AtalaObjectId.of(node_internal.AtalaObject(blockOperationCount = ind))
          }
      }
    }
  }

  private def insert(objectId: AtalaObjectId, byteContent: Array[Byte]): Unit = {
    AtalaObjectsDAO
      .insert(AtalaObjectsDAO.AtalaObjectCreateData(objectId, byteContent))
      .transact(database)
      .unsafeToFuture()
      .void
      .futureValue
  }

  private def get(objectId: AtalaObjectId): AtalaObjectInfo = {
    AtalaObjectsDAO.get(objectId).transact(database).unsafeRunSync().value
  }
}
