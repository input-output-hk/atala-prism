package io.iohk.atala.prism.node.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.node.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, KeyUsage}
import org.scalatest.OptionValues._

import java.time.Instant
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.node.{AtalaWithPostgresSpec, DataPreparation}
import tofu.logging.Logging.Make
import tofu.logging.Logging

class DIDDataRepositorySpec extends AtalaWithPostgresSpec {
  val logs: Make[IO] = Logging.Make.plain[IO]
  lazy val didDataRepository: DIDDataRepository[IO] =
    DIDDataRepository.unsafe(database, logs)

  val operationDigest = digestGen(0, 1)
  val didSuffix = didSuffixFromDigest(operationDigest)

  val keys = List(
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "master",
      keyUsage = KeyUsage.MasterKey,
      key = CryptoTestUtils.generatePublicKeyData()
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "issuing",
      keyUsage = KeyUsage.IssuingKey,
      key = CryptoTestUtils.generatePublicKeyData()
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "authentication",
      keyUsage = KeyUsage.AuthenticationKey,
      key = CryptoTestUtils.generatePublicKeyData()
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "keyAgreement",
      keyUsage = KeyUsage.KeyAgreementKey,
      key = CryptoTestUtils.generatePublicKeyData()
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

  "DIDDataRepository" should {
    "retrieve previously inserted DID data" in {
      DataPreparation.createDID(didData, dummyLedgerData)
      val did = didDataRepository
        .findByDid(CryptoTestUtils.buildCanonicalDID(operationDigest))
        .unsafeRunSync()
        .toOption
        .value

      did.value.didSuffix mustBe didSuffix
    }

    "return empty DID document when the DID suffix is not found" in {
      DataPreparation.createDID(didData, dummyLedgerData)

      val result = didDataRepository
        .findByDid(
          CryptoTestUtils.buildCanonicalDID(
            Sha256Hash.fromHex(didSuffixFromDigest(digestGen(0, 2)).value)
          )
        )
        .unsafeRunSync()
        .toOption
        .value

      result must be(empty)
    }
  }
}
