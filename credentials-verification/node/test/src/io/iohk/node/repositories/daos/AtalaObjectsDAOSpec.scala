package io.iohk.node.repositories.daos

import doobie.implicits._
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.cvp.repositories.PostgresRepositorySpec
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class AtalaObjectsDAOSpec extends PostgresRepositorySpec {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

  override val tables = List("atala_objects")

  val objectIds = (1 to 9).map(i => SHA256Digest.compute(s"object$i".getBytes))
  val objectId = objectIds(0)

  val blockHashes = (1 to 9).map(i => SHA256Digest.compute(s"block$i".getBytes))
  val blockHash = blockHashes(0)

  "AtalaObjectsDAO" should {
    "retrieve inserted objects" in {
      AtalaObjectsDAO.insert(AtalaObjectsDAO.AtalaObjectCreateData(objectId, 1)).transact(database).unsafeRunSync()
      val retrieved = AtalaObjectsDAO.get(objectId).transact(database).unsafeRunSync().value
      retrieved.objectId mustBe objectId
      retrieved.sequenceNumber mustBe 1
      retrieved.processed mustBe false
      retrieved.blockHash mustBe None
    }

    "mark object as processed" in {
      AtalaObjectsDAO.insert(AtalaObjectsDAO.AtalaObjectCreateData(objectId, 1)).transact(database).unsafeRunSync()
      AtalaObjectsDAO.setProcessed(objectId).transact(database).unsafeRunSync()
      val retrieved = AtalaObjectsDAO.get(objectId).transact(database).unsafeRunSync().value
      retrieved.processed mustBe true
    }

    "add block hash to the object" in {
      AtalaObjectsDAO.insert(AtalaObjectsDAO.AtalaObjectCreateData(objectId, 1)).transact(database).unsafeRunSync()
      AtalaObjectsDAO.setBlockHash(objectId, blockHash).transact(database).unsafeRunSync()
      val retrieved = AtalaObjectsDAO.get(objectId).transact(database).unsafeRunSync().value
      retrieved.blockHash mustBe Some(blockHash)
    }

    "return object with highest sequence number" in {
      for ((objId, zeroBasedI) <- objectIds.zipWithIndex) {
        AtalaObjectsDAO
          .insert(AtalaObjectsDAO.AtalaObjectCreateData(objId, zeroBasedI + 1))
          .transact(database)
          .unsafeRunSync()
      }

      val retrieved = AtalaObjectsDAO.getNewest().transact(database).unsafeRunSync().value
      retrieved.sequenceNumber mustBe objectIds.size
    }
  }
}
