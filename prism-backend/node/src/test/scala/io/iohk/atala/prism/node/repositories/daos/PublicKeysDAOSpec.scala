package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, KeyUsage}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.repositories.{didSuffixFromDigest, digestGen}
import org.scalatest.OptionValues._

class PublicKeysDAOSpec extends AtalaWithPostgresSpec {

  val operationDigest = digestGen(0, 1)
  val didSuffix = didSuffixFromDigest(operationDigest)

  val keys = List(
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "master",
      keyUsage = KeyUsage.MasterKey,
      key = EC.generateKeyPair().getPublicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "issuing",
      keyUsage = KeyUsage.IssuingKey,
      key = EC.generateKeyPair().getPublicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "authentication",
      keyUsage = KeyUsage.AuthenticationKey,
      key = EC.generateKeyPair().getPublicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "communication",
      keyUsage = KeyUsage.CommunicationKey,
      key = EC.generateKeyPair().getPublicKey
    )
  )

  val didData = DIDData(didSuffix, keys, operationDigest)
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
        DIDData(didSuffix, keys.tail, operationDigest),
        dummyLedgerData
      )
      val result = DataPreparation.findKey(didSuffix, "master")

      result must be(empty)
    }
  }
}
