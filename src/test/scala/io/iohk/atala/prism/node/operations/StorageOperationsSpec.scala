package io.iohk.atala.prism.node.operations

import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.node.AtalaWithPostgresSpec
import io.iohk.atala.prism.node.DataPreparation.{dummyApplyOperationConfig, dummyLedgerData}
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.models.StorageData.Bytes
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.operations.StateError.{EntityMissing, InvalidKeyUsed, InvalidPreviousOperation}
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO, VdrEntriesDAO}
import io.iohk.atala.prism.protos.node_models
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

class StorageOperationsSpec extends AtalaWithPostgresSpec {

  private val didHash = Sha256Hash.compute("did".getBytes())
  private val didSuffix = DidSuffix(didHash.hexEncoded)
  private val vdrKeyPair = CryptoTestUtils.generateKeyPair()

  private def insertDid(): Unit =
    DIDDataDAO
      .insert(didSuffix, didHash, dummyLedgerData)
      .transact(database)
      .unsafeRunSync()

  private def insertVdrKey(keyId: String = "vdr"): Unit =
    PublicKeysDAO
      .insert(
        DIDPublicKey(didSuffix, keyId, KeyUsage.VDRSigningKey, CryptoTestUtils.toPublicKeyData(vdrKeyPair.publicKey)),
        dummyLedgerData
      )
      .transact(database)
      .unsafeRunSync()

  private def insertMasterKey(): Unit =
    PublicKeysDAO
      .insert(
        DIDPublicKey(didSuffix, "master", KeyUsage.MasterKey, CryptoTestUtils.toPublicKeyData(vdrKeyPair.publicKey)),
        dummyLedgerData
      )
      .transact(database)
      .unsafeRunSync()

  private def createStorageProto(data: node_models.StorageData): node_models.AtalaOperation =
    node_models
      .AtalaOperation()
      .withCreateStorageEntry(
        node_models
          .CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didHash.bytes.toArray))
          .withNonce(ByteString.copyFromUtf8("nonce"))
          .withData(data)
      )

  private def updateStorageProto(previous: Sha256Hash, data: node_models.StorageData): node_models.AtalaOperation =
    node_models
      .AtalaOperation()
      .withUpdateStorageEntry(
        node_models
          .UpdateStorageEntryOperation()
          .withPreviousEventHash(ByteString.copyFrom(previous.bytes.toArray))
          .withData(data)
      )

  private def deactivateStorageProto(previous: Sha256Hash): node_models.AtalaOperation =
    node_models
      .AtalaOperation()
      .withDeactivateStorageEntry(
        node_models
          .DeactivateStorageEntryOperation()
          .withPreviousEventHash(ByteString.copyFrom(previous.bytes.toArray))
      )

  "StorageOperations.parseCreate" should {
    "extract bytes payload, nonce and digest" in {
      val data = node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload"))
      val proto = createStorageProto(data)

      val op = StorageOperations.parseCreate(proto, dummyLedgerData).value

      op.didSuffix mustBe didSuffix
      op.nonce.value mustBe "nonce".getBytes.toVector
      op.data mustBe Bytes("payload".getBytes.toVector)
      op.digest mustBe Sha256Hash.compute(proto.toByteArray)
    }

    "fail when storage data is missing" in {
      val proto = createStorageProto(node_models.StorageData())

      val parsed = StorageOperations.parseCreate(proto, dummyLedgerData)

      parsed.left.value mustBe a[ValidationError.InvalidValue]
    }
  }

  "StorageOperations.getCorrectnessData" should {
    "fail when DID is missing" in {
      val data = node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload"))
      val op = StorageOperations.parseCreate(createStorageProto(data), dummyLedgerData).value

      val res = op.getCorrectnessData("vdr").value.transact(database).unsafeRunSync()

      res.left.value mustBe EntityMissing("did suffix", didSuffix.getValue)
    }

    "fail when key usage is not VDR signing" in {
      insertDid()
      insertMasterKey()
      val data = node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload"))
      val op = StorageOperations.parseCreate(createStorageProto(data), dummyLedgerData).value

      val res = op.getCorrectnessData("master").value.transact(database).unsafeRunSync()

      res.left.value mustBe InvalidKeyUsed("VDR signing key")
    }

    "succeed with an active VDR signing key" in {
      insertDid()
      insertVdrKey()
      val data = node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload"))
      val op = StorageOperations.parseCreate(createStorageProto(data), dummyLedgerData).value

      val res = op.getCorrectnessData("vdr").value.transact(database).unsafeRunSync()

      res.value.key.compressed.toVector mustBe vdrKeyPair.publicKey.compressed.toVector
    }
  }

  "StorageOperations.applyState" should {
    "persist a create entry with BYTES payload" in {
      insertDid()
      insertVdrKey()
      val payload = "payload".getBytes
      val op = StorageOperations
        .parseCreate(
          createStorageProto(node_models.StorageData().withBytes(ByteString.copyFrom(payload))),
          dummyLedgerData
        )
        .value

      op.applyState(dummyApplyOperationConfig).value.transact(database).unsafeRunSync().value

      val stored = VdrEntriesDAO.find(op.digest).transact(database).unsafeRunSync().value
      stored.didSuffix mustBe didSuffix
      stored.status mustBe VdrEntryStatus.ACTIVE
      stored.dataType mustBe "BYTES"
      stored.dataBytes.value mustBe payload
      stored.previousEventHash mustBe None
      stored.nonce.value mustBe "nonce".getBytes
    }

    "persist an update entry linking the previous event" in {
      insertDid()
      insertVdrKey()
      val initial = StorageOperations
        .parseCreate(
          createStorageProto(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload"))),
          dummyLedgerData
        )
        .value
      initial.applyState(dummyApplyOperationConfig).value.transact(database).unsafeRunSync().value

      val updateProto = updateStorageProto(initial.digest, node_models.StorageData().withIpfsCid("cid123"))
      val updateOp = StorageOperations.parseUpdate(updateProto, dummyLedgerData).value
      updateOp.applyState(dummyApplyOperationConfig).value.transact(database).unsafeRunSync().value

      val stored = VdrEntriesDAO.find(updateOp.digest).transact(database).unsafeRunSync().value
      stored.previousEventHash.value mustBe initial.digest
      stored.dataType mustBe "IPFS"
      stored.dataIpfs.value mustBe "cid123"
      stored.status mustBe VdrEntryStatus.ACTIVE
    }

    "reject operations referencing a deactivated entry" in {
      insertDid()
      insertVdrKey()
      val createOp = StorageOperations
        .parseCreate(
          createStorageProto(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload"))),
          dummyLedgerData
        )
        .value
      createOp.applyState(dummyApplyOperationConfig).value.transact(database).unsafeRunSync().value

      val deactivateOp =
        StorageOperations.parseDeactivate(deactivateStorageProto(createOp.digest), dummyLedgerData).value
      deactivateOp.applyState(dummyApplyOperationConfig).value.transact(database).unsafeRunSync().value

      val updateAfterDeactivate =
        StorageOperations
          .parseUpdate(
            updateStorageProto(deactivateOp.digest, node_models.StorageData().withBytes(ByteString.EMPTY)),
            dummyLedgerData
          )
          .value

      val res = updateAfterDeactivate.getCorrectnessData("vdr").value.transact(database).unsafeRunSync()

      res.left.value mustBe InvalidPreviousOperation()
    }
  }
}
