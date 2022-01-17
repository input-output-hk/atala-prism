package io.iohk.atala.prism.node.repositories.daos

import cats.effect.unsafe.implicits.global
import cats.syntax.functor._
import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.models.{BlockInfo, Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.models.{AtalaObjectId, AtalaObjectInfo, AtalaObjectStatus}
import io.iohk.atala.prism.protos.node_internal
import org.scalatest.OptionValues._

import java.time.Instant

class AtalaObjectsDAOSpec extends AtalaWithPostgresSpec {
  private val objectId = AtalaObjectId.of(node_internal.AtalaObject())
  private val byteContent = "byteContent".getBytes
  private val transactionInfo = TransactionInfo(
    transactionId = TransactionId
      .from(Sha256.compute("transactionId".getBytes).getValue)
      .value,
    ledger = Ledger.InMemory,
    block = Some(
      BlockInfo(
        number = 1,
        timestamp = Instant.ofEpochMilli(133713371337L),
        index = 0
      )
    )
  )

  "AtalaObjectsDAO.insert" should {
    "insert an object" in {
      insert(objectId, byteContent)

      val retrieved = get(objectId)
      retrieved.objectId mustBe objectId
      retrieved.byteContent mustBe byteContent
      retrieved.transaction mustBe None
      retrieved.status mustBe AtalaObjectStatus.Scheduled
    }
  }

  "AtalaObjectsDAO.setTransactionInfo" should {
    "set the transaction info of an existing object" in {
      insert(objectId, byteContent)

      AtalaObjectsDAO
        .setTransactionInfo(
          AtalaObjectsDAO
            .AtalaObjectSetTransactionInfo(objectId, transactionInfo)
        )
        .transact(database)
        .unsafeRunSync()

      val retrieved = get(objectId)
      retrieved.objectId mustBe objectId
      retrieved.byteContent mustBe byteContent
      retrieved.transaction.value mustBe transactionInfo
      retrieved.status mustBe AtalaObjectStatus.Scheduled
    }

    "fail to set the transaction info of a nonexistent object" in {
      assertThrows[Exception] {
        AtalaObjectsDAO
          .setTransactionInfo(
            AtalaObjectsDAO
              .AtalaObjectSetTransactionInfo(objectId, transactionInfo)
          )
          .transact(database)
          .unsafeRunSync()
      }
    }

    "fail to set the transaction info without a block" in {
      insert(objectId, byteContent)

      assertThrows[Exception] {
        AtalaObjectsDAO
          .setTransactionInfo(
            AtalaObjectsDAO.AtalaObjectSetTransactionInfo(
              objectId,
              transactionInfo.copy(block = None)
            )
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
      retrieved.status mustBe AtalaObjectStatus.Scheduled
    }

    "get an object with transaction info" in {
      insert(objectId, byteContent)
      AtalaObjectsDAO
        .setTransactionInfo(
          AtalaObjectsDAO
            .AtalaObjectSetTransactionInfo(objectId, transactionInfo)
        )
        .transact(database)
        .unsafeRunSync()

      val retrieved = get(objectId)

      retrieved.objectId mustBe objectId
      retrieved.byteContent mustBe byteContent
      retrieved.transaction.value mustBe transactionInfo
      retrieved.status mustBe AtalaObjectStatus.Scheduled
    }
  }

  "AtalaObjectsDAO.setProcessed" should {
    "mark object as processed" in {
      insert(objectId, byteContent)

      AtalaObjectsDAO
        .updateObjectStatus(objectId, AtalaObjectStatus.Processed)
        .transact(database)
        .unsafeRunSync()

      val retrieved = get(objectId)
      retrieved.status mustBe AtalaObjectStatus.Processed
    }
  }

  "AtalaObjectsDAO.getNotPublishedObjectIds" should {
    "return object ids in the correct order" in {
      val N = 10
      (0 until N).foreach { count =>
        val objId = AtalaObjectId.of(
          node_internal.AtalaObject(blockOperationCount = count)
        )
        insert(objId, byteContent)
      }
      AtalaObjectsDAO
        .updateObjectStatus(AtalaObjectStatus.Scheduled, AtalaObjectStatus.Pending)
        .transact(database)
        .unsafeRunSync()
      val retrieved = AtalaObjectsDAO.getNotPublishedObjectInfos
        .transact(database)
        .unsafeRunSync()
      retrieved.size mustBe N
      retrieved.zipWithIndex.foreach { case (objInfo, ind) =>
        withClue(s"Index $ind:") {
          objInfo.objectId mustBe AtalaObjectId.of(
            node_internal.AtalaObject(blockOperationCount = ind)
          )
        }
      }
    }
  }

  private def insert(
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

  private def get(objectId: AtalaObjectId): AtalaObjectInfo = {
    AtalaObjectsDAO.get(objectId).transact(database).unsafeRunSync().value
  }
}
