package io.iohk.atala.prism.node.repositories

import cats.effect.unsafe.implicits.global
import doobie.implicits._
import io.iohk.atala.prism.node.{AtalaWithPostgresSpec, DataPreparation}
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.models.StorageData.Bytes
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO, VdrEntriesDAO}
import org.scalatest.OptionValues._

/** Lightweight repository-level check that heads are updated on create/update/deactivate. */
class VdrEntriesRepositorySpec extends AtalaWithPostgresSpec {

  private val didHash = Sha256Hash.compute("repo-did".getBytes())
  private val didSuffix = DidSuffix(didHash.hexEncoded)
  private val vdrKey = CryptoTestUtils.generateKeyPair()
  private val ledgerData = DataPreparation.dummyLedgerData

  private lazy val repo = new VdrEntriesRepositoryImpl(database)

  override def beforeEach(): Unit = {
    super.beforeEach()
    DIDDataDAO.insert(didSuffix, didHash, ledgerData).transact(database).unsafeRunSync()
    PublicKeysDAO
      .insert(
        DIDPublicKey(didSuffix, "vdr", KeyUsage.VDRKey, CryptoTestUtils.toPublicKeyData(vdrKey.publicKey)),
        ledgerData
      )
      .transact(database)
      .unsafeRunSync()
  }

  "VdrEntriesRepository" should {
    "update head to DEACTIVATED on deactivate" in {
      val createHash = Sha256Hash.compute("repo-create".getBytes)
      val updateHash = Sha256Hash.compute("repo-update".getBytes)
      val deactivateHash = Sha256Hash.compute("repo-deactivate".getBytes)

      repo
        .insertCreate(createHash, didSuffix, None, Bytes("v1".getBytes.toVector), ledgerData)
        .unsafeRunSync()
      repo
        .insertUpdate(updateHash, didSuffix, createHash, Bytes("v2".getBytes.toVector), ledgerData)
        .unsafeRunSync()
      repo
        .insertDeactivate(deactivateHash, didSuffix, updateHash, ledgerData)
        .unsafeRunSync()

      val head = VdrEntriesDAO.findHead(createHash).transact(database).unsafeRunSync().value
      head._1 mustBe deactivateHash
      head._2 mustBe VdrEntryStatus.DEACTIVATED
    }
  }
}
