package io.iohk.atala.prism.node.repositories.daos

import doobie.implicits._
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.repositories.{didSuffixFromDigest, digestGen}
import io.iohk.atala.prism.node.{AtalaWithPostgresSpec, DataPreparation}
import org.scalatest.OptionValues._

class VdrEntriesDAOSpec extends AtalaWithPostgresSpec {

  private val didOperationDigest = digestGen(0, 1)
  private val didSuffix = didSuffixFromDigest(didOperationDigest)
  private val masterKey = DIDPublicKey(
    didSuffix = didSuffix,
    keyId = "master",
    keyUsage = KeyUsage.MasterKey,
    key = CryptoTestUtils.generatePublicKeyData()
  )
  private val didData = DIDData(didSuffix, List(masterKey), Nil, Nil, didOperationDigest)
  private val ledgerData: LedgerData = DataPreparation.dummyLedgerData

  private val eventHash = Sha256Hash.compute("event-1".getBytes)
  private val prevHash = Sha256Hash.compute("event-0".getBytes)
  private val deactivateHash = Sha256Hash.compute("event-2".getBytes)

  override def beforeEach(): Unit = {
    super.beforeEach()
    DataPreparation.createDID(didData, ledgerData)
  }

  "VdrEntriesDAO" should {
    "insert and fetch BYTES storage entry" in {
      val payload = "payload".getBytes

      VdrEntriesDAO
        .insert(
          eventHash,
          eventHash,
          didSuffix,
          nonce = Some("n1".getBytes),
          dataType = "BYTES",
          dataBytes = Some(payload),
          dataIpfs = None,
          previousEventHash = None,
          status = VdrEntryStatus.ACTIVE,
          ledgerData = ledgerData
        )
        .transact(database)
        .unsafeRunSync()

      val stored = VdrEntriesDAO.find(eventHash).transact(database).unsafeRunSync().value
      stored.eventHash mustBe eventHash
      stored.didSuffix mustBe didSuffix
      stored.dataType mustBe "BYTES"
      stored.dataBytes.value mustBe payload
      stored.dataIpfs mustBe empty
      stored.previousEventHash mustBe empty
      stored.status mustBe VdrEntryStatus.ACTIVE
      stored.nonce.value mustBe "n1".getBytes
    }

    "insert update with IPFS data and link to previous event" in {
      VdrEntriesDAO
        .insert(
          prevHash,
          prevHash,
          didSuffix,
          nonce = None,
          dataType = "BYTES",
          dataBytes = Some("old".getBytes),
          dataIpfs = None,
          previousEventHash = None,
          status = VdrEntryStatus.ACTIVE,
          ledgerData = ledgerData
        )
        .transact(database)
        .unsafeRunSync()

      VdrEntriesDAO
        .insert(
          prevHash,
          eventHash,
          didSuffix,
          nonce = None,
          dataType = "IPFS",
          dataBytes = None,
          dataIpfs = Some("cid123"),
          previousEventHash = Some(prevHash),
          status = VdrEntryStatus.ACTIVE,
          ledgerData = ledgerData
        )
        .transact(database)
        .unsafeRunSync()

      val stored = VdrEntriesDAO.find(eventHash).transact(database).unsafeRunSync().value
      stored.previousEventHash.value mustBe prevHash
      stored.dataType mustBe "IPFS"
      stored.dataBytes mustBe empty
      stored.dataIpfs.value mustBe "cid123"
      stored.status mustBe VdrEntryStatus.ACTIVE
    }

    "store deactivation entries with NONE data" in {
      VdrEntriesDAO
        .insert(
          prevHash,
          prevHash,
          didSuffix,
          nonce = None,
          dataType = "BYTES",
          dataBytes = Some("old".getBytes),
          dataIpfs = None,
          previousEventHash = None,
          status = VdrEntryStatus.ACTIVE,
          ledgerData = ledgerData
        )
        .transact(database)
        .unsafeRunSync()

      VdrEntriesDAO
        .insert(
          prevHash,
          deactivateHash,
          didSuffix,
          nonce = None,
          dataType = "NONE",
          dataBytes = None,
          dataIpfs = None,
          previousEventHash = Some(prevHash),
          status = VdrEntryStatus.DEACTIVATED,
          ledgerData = ledgerData
        )
        .transact(database)
        .unsafeRunSync()

      val stored = VdrEntriesDAO.find(deactivateHash).transact(database).unsafeRunSync().value
      stored.status mustBe VdrEntryStatus.DEACTIVATED
      stored.dataBytes mustBe empty
      stored.dataIpfs mustBe empty
      stored.previousEventHash.value mustBe prevHash
    }

    "maintain head pointers for create/update/deactivate chain" in {
      val root = Sha256Hash.compute("head-root".getBytes)
      val update = Sha256Hash.compute("head-update".getBytes)
      val deactivate = Sha256Hash.compute("head-deactivate".getBytes)

      val insert = (hash: Sha256Hash, prev: Option[Sha256Hash], status: VdrEntryStatus, data: String) =>
        VdrEntriesDAO
          .insert(
            prev.getOrElse(hash),
            hash,
            didSuffix,
            nonce = None,
            dataType = "BYTES",
            dataBytes = Some(data.getBytes),
            dataIpfs = None,
            previousEventHash = prev,
            status = status,
            ledgerData = ledgerData
          )
          .transact(database)
          .unsafeRunSync()

      insert(root, None, VdrEntryStatus.ACTIVE, "v1")
      VdrEntriesDAO.insertHead(root, root, VdrEntryStatus.ACTIVE).transact(database).unsafeRunSync()

      insert(update, Some(root), VdrEntryStatus.ACTIVE, "v2")
      VdrEntriesDAO.updateHead(root, update, VdrEntryStatus.ACTIVE).transact(database).unsafeRunSync()

      insert(deactivate, Some(update), VdrEntryStatus.DEACTIVATED, "")
      VdrEntriesDAO.updateHead(root, deactivate, VdrEntryStatus.DEACTIVATED).transact(database).unsafeRunSync()

      val head = VdrEntriesDAO.findHead(root).transact(database).unsafeRunSync().value
      head._1 mustBe deactivate
      head._2 mustBe VdrEntryStatus.DEACTIVATED
    }

  }
}
