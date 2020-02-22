package io.iohk.node.repositories

import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.errors.NodeError.UnknownValueError
import io.iohk.node.models.{DIDData, DIDPublicKey, KeyUsage}
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationLong

class DIDDataRepositorySpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)
  lazy val didDataRepository = new DIDDataRepository(database)

  override val tables = List("public_keys", "did_data")

  val operationDigest = digestGen(0, 1)
  val didSuffix = didSuffixFromDigest(operationDigest)

  val keys = List(
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "master",
      keyUsage = KeyUsage.MasterKey,
      key = ECKeys.generateKeyPair().getPublic
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "issuing",
      keyUsage = KeyUsage.IssuingKey,
      key = ECKeys.generateKeyPair().getPublic
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "authentication",
      keyUsage = KeyUsage.AuthenticationKey,
      key = ECKeys.generateKeyPair().getPublic
    ),
    DIDPublicKey(
      didSuffix = didSuffix,
      keyId = "communication",
      keyUsage = KeyUsage.CommunicationKey,
      key = ECKeys.generateKeyPair().getPublic
    )
  )

  val didData = DIDData(didSuffix, keys, operationDigest)

  "DIDDataRepository" should {
    "retrieve previously inserted DID data" in {
      val result = (for {
        _ <- didDataRepository.create(didData)
        did <- didDataRepository.findByDidSuffix(didSuffix)
      } yield did).value.futureValue.right.value

      result.didSuffix mustBe didSuffix
    }

    "return UnknownValueError when the DID is not found" in {
      val result = (for {
        _ <- didDataRepository.create(didData)
        did <- didDataRepository.findByDidSuffix(didSuffixFromDigest(digestGen(0, 2)))
      } yield did).value.futureValue.left.value

      result must be(a[UnknownValueError])
    }

    "retrieve previously inserted DID key" in {
      val result = (for {
        _ <- didDataRepository.create(didData)
        key <- didDataRepository.findKey(didSuffix, "issuing")
      } yield key).value.futureValue.right.value

      result mustBe keys.tail.head
    }

    "return UnknownValueError when retrieving key for non-existing DID" in {
      val result = (for {
        _ <- didDataRepository.create(didData)
        key <- didDataRepository.findKey(didSuffixFromDigest(digestGen(0, 2)), "issuing")
      } yield key).value.futureValue.left.value

      result must be(a[UnknownValueError])
    }

    "return UnknownValueError when retrieving non-existing key" in {
      val result = (for {
        _ <- didDataRepository.create(DIDData(didSuffix, keys.tail, operationDigest))
        key <- didDataRepository.findKey(didSuffix, "master")
      } yield key).value.futureValue.left.value

      result must be(a[UnknownValueError])

    }
  }

}
