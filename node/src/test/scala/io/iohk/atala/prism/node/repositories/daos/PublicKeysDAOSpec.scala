package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.repositories.{didSuffixFromDigest, digestGen}
import org.scalatest.OptionValues._
import identus.apollo.MyKeyPair

class PublicKeysDAOSpec extends AtalaWithPostgresSpec {

  val operationDigest = digestGen(0, 1)
  val didSuffix = didSuffixFromDigest(operationDigest)

  val keys = List(
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "master",
      keyUsage = KeyUsage.MasterKey,
      key = MyKeyPair.generateKeyPair.publicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "issuing",
      keyUsage = KeyUsage.IssuingKey,
      key = MyKeyPair.generateKeyPair.publicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "authentication",
      keyUsage = KeyUsage.AuthenticationKey,
      key = MyKeyPair.generateKeyPair.publicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "keyAgreement",
      keyUsage = KeyUsage.KeyAgreementKey,
      key = MyKeyPair.generateKeyPair.publicKey
    )
  )

  val didData = DIDData(didSuffix, keys, Nil, Nil, operationDigest)
  val dummyTimestamp =
    new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
  val dummyLedgerData = LedgerData(
    TransactionId
      .from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0))
      .value,
    Ledger.InMemory,
    dummyTimestamp
  )

  "PublicKeysDAO" should {
    "retrieve previously inserted DID key" in {
      DataPreparation.createDID(didData, dummyLedgerData)
      val result = DataPreparation.findKey(didSuffix, "issuing").value

      DIDPublicKey(
        result.didSuffix,
        result.keyId,
        result.keyUsage,
        result.key
      ) mustBe keys.tail.head
      result.addedOn mustBe dummyLedgerData
      result.revokedOn mustBe None
    }

    "retrieve the correct revocation details" in {
      val revocationTimestamp = new TimestampInfo(Instant.ofEpochMilli(1000).toEpochMilli, 10, 1)
      val revocationLedgerData = LedgerData(
        TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(1)).value,
        Ledger.InMemory,
        revocationTimestamp
      )
      val keyId = "issuing"

      DataPreparation.createDID(didData, dummyLedgerData)
      DataPreparation.revokeKey(didSuffix, keyId, revocationLedgerData)
      val result = DataPreparation.findKey(didSuffix, keyId).value

      result.revokedOn.value mustBe revocationLedgerData
    }

    "retrieve all previously inserted DID key for given suffix" in {
      DataPreparation.createDID(didData, dummyLedgerData)
      val results = DataPreparation.findByDidSuffix(didSuffix).keys

      results
        .map(didPublicKeyState =>
          DIDPublicKey(
            didPublicKeyState.didSuffix,
            didPublicKeyState.keyId,
            didPublicKeyState.keyUsage,
            didPublicKeyState.key
          )
        )
        .sortWith((l, r) => l.keyId < r.keyId) mustBe keys.sortWith((l, r) => l.keyId < r.keyId)

      results.foreach(didPublicKeyState => didPublicKeyState.addedOn mustBe dummyLedgerData)
      results.foreach(didPublicKeyState => didPublicKeyState.revokedOn mustBe None)
    }

    "return None when retrieving key for non-existing DID" in {
      DataPreparation.createDID(didData, dummyLedgerData)
      val result =
        DataPreparation.findKey(didSuffixFromDigest(digestGen(0, 2)), "issuing")

      result must be(empty)
    }

    "return None when retrieving non-existing key" in {
      DataPreparation.createDID(
        DIDData(didSuffix, keys.tail, Nil, Nil, operationDigest),
        dummyLedgerData
      )
      val result = DataPreparation.findKey(didSuffix, "master")

      result must be(empty)
    }
  }
}
