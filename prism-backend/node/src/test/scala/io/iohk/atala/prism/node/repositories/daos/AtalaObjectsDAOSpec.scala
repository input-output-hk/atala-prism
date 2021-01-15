package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant

import doobie.implicits._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{BlockInfo, Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.models.{AtalaObject, AtalaObjectId}
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.AtalaWithPostgresSpec
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class AtalaObjectsDAOSpec extends AtalaWithPostgresSpec {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

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

  private def insert(objectId: AtalaObjectId, byteContent: Array[Byte]): Unit = {
    AtalaObjectsDAO
      .insert(AtalaObjectsDAO.AtalaObjectCreateData(objectId, byteContent))
      .transact(database)
      .unsafeRunSync()
  }

  private def get(objectId: AtalaObjectId): AtalaObject = {
    AtalaObjectsDAO.get(objectId).transact(database).unsafeRunSync().value
  }
}
