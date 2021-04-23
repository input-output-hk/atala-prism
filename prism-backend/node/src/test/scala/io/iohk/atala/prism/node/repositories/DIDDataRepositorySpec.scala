package io.iohk.atala.prism.node.repositories

import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, KeyUsage}
import org.scalatest.OptionValues._
import java.time.Instant

import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.node.DataPreparation

class DIDDataRepositorySpec extends AtalaWithPostgresSpec {
  lazy val didDataRepository = new DIDDataRepository(database)

  val operationDigest = digestGen(0, 1)
  val didSuffix = didSuffixFromDigest(operationDigest)

  val keys = List(
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "master",
      keyUsage = KeyUsage.MasterKey,
      key = EC.generateKeyPair().publicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "issuing",
      keyUsage = KeyUsage.IssuingKey,
      key = EC.generateKeyPair().publicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "authentication",
      keyUsage = KeyUsage.AuthenticationKey,
      key = EC.generateKeyPair().publicKey
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "communication",
      keyUsage = KeyUsage.CommunicationKey,
      key = EC.generateKeyPair().publicKey
    )
  )

  val didData = DIDData(didSuffix, keys, operationDigest)
  val dummyTimestamp = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
  val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    dummyTimestamp
  )

  "DIDDataRepository" should {
    "retrieve previously inserted DID data" in {
      DataPreparation.createDID(didData, dummyLedgerData)
      val did = didDataRepository.findByDid(DID.buildPrismDID(didSuffix)).value.futureValue.toOption.value

      did.value.didSuffix mustBe didSuffix
    }

    "return empty DID document when the DID suffix is not found" in {
      DataPreparation.createDID(didData, dummyLedgerData)

      val result = didDataRepository
        .findByDid(DID.buildPrismDID(didSuffixFromDigest(digestGen(0, 2))))
        .value
        .futureValue
        .toOption
        .value

      result must be(empty)
    }

    "return error when did is in invalid format" in {
      val did = io.iohk.atala.prism.identity.DID.buildPrismDID("11:11:11:11")
      didDataRepository.findByDid(did).value.futureValue mustBe a[Left[UnknownValueError, _]]
    }
  }

}
