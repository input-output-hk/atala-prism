package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant

import doobie.implicits._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class AtalaObjectsDAOSpec extends PostgresRepositorySpec {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

  private val objectIds = (1 to 9).map(i => SHA256Digest.compute(s"object$i".getBytes))
  private val objectId = objectIds(0)

  private val objectTimestamp = Instant.ofEpochMilli(133713371337L)
  private val transactionId = TransactionId.from(SHA256Digest.compute("transactionId".getBytes).value).value
  private val ledger = Ledger.InMemory

  "AtalaObjectsDAO" should {
    "retrieve inserted objects" in {
      AtalaObjectsDAO
        .insert(AtalaObjectsDAO.AtalaObjectCreateData(objectId, 1, objectTimestamp, None, transactionId, ledger))
        .transact(database)
        .unsafeRunSync()
      val retrieved = AtalaObjectsDAO.get(objectId).transact(database).unsafeRunSync().value
      retrieved.objectId mustBe objectId
      retrieved.sequenceNumber mustBe 1
      retrieved.objectTimestamp mustBe objectTimestamp
      retrieved.byteContent mustBe None
      retrieved.transactionId mustBe transactionId
      retrieved.ledger mustBe ledger
      retrieved.processed mustBe false
    }

    "mark object as processed" in {
      AtalaObjectsDAO
        .insert(AtalaObjectsDAO.AtalaObjectCreateData(objectId, 1, objectTimestamp, None, transactionId, ledger))
        .transact(database)
        .unsafeRunSync()
      AtalaObjectsDAO.setProcessed(objectId).transact(database).unsafeRunSync()
      val retrieved = AtalaObjectsDAO.get(objectId).transact(database).unsafeRunSync().value
      retrieved.processed mustBe true
    }

    "return object with highest sequence number" in {
      for ((objId, zeroBasedI) <- objectIds.zipWithIndex) {
        AtalaObjectsDAO
          .insert(
            AtalaObjectsDAO.AtalaObjectCreateData(objId, zeroBasedI + 1, objectTimestamp, None, transactionId, ledger)
          )
          .transact(database)
          .unsafeRunSync()
      }

      val retrieved = AtalaObjectsDAO.getNewest().transact(database).unsafeRunSync().value
      retrieved.sequenceNumber mustBe objectIds.size
    }
  }
}
