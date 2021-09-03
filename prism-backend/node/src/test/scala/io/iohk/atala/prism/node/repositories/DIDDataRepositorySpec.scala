package io.iohk.atala.prism.node.repositories

import cats.effect.IO
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.kotlin.credentials.TimestampInfo
import io.iohk.atala.prism.kotlin.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.kotlin.crypto.Sha256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.models.{DIDData, DIDPublicKey, KeyUsage}
import org.scalatest.OptionValues._

import java.time.Instant
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.utils.StringUtils.encodeToByteArray

class DIDDataRepositorySpec extends AtalaWithPostgresSpec {
  lazy val didDataRepository: DIDDataRepository[IO] = DIDDataRepository(database)

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
  val dummyTimestamp = new TimestampInfo(Instant.ofEpochMilli(0).toEpochMilli, 1, 0)
  val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    dummyTimestamp
  )

  "DIDDataRepository" should {
    "retrieve previously inserted DID data" in {
      DataPreparation.createDID(didData, dummyLedgerData)
      val did = didDataRepository.findByDid(PrismDid.buildCanonical(SHA256Digest.compute(encodeToByteArray(didSuffix)))).unsafeRunSync().toOption.value

      did.value.didSuffix mustBe didSuffix
    }

    "return empty DID document when the DID suffix is not found" in {
      DataPreparation.createDID(didData, dummyLedgerData)

      val result = didDataRepository
        .findByDid(PrismDid.buildCanonical(SHA256Digest.compute(encodeToByteArray(didSuffixFromDigest(digestGen(0, 2))))))
        .unsafeRunSync()
        .toOption
        .value

      result must be(empty)
    }

    "return error when did is in invalid format" in {
      val did = PrismDid.buildCanonical(SHA256Digest.compute(encodeToByteArray("11:11:11:11")))
      didDataRepository.findByDid(did).unsafeToFuture().futureValue mustBe a[Left[UnknownValueError, _]]
    }
  }

}
