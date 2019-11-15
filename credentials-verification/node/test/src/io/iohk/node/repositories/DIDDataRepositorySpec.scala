package io.iohk.node.repositories

import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.errors.NodeError.UnknownValueError
import io.iohk.node.models.{DIDPublicKey, DIDSuffix, KeyUsage}
import org.scalatest.EitherValues._

import scala.concurrent.duration.DurationLong

class DIDDataRepositorySpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)
  lazy val didDataRepository = new DIDDataRepository(database)

  override val tables = List("public_keys", "did_data")

  def bytesGen(i: Int) = Array.fill(31)(0.toByte) :+ i.toByte
  def didSuffixFromDigest(bytes: Array[Byte]) = DIDSuffix(bytes.map("%02x".format(_)).mkString(""))

  val operationDigest = bytesGen(1)
  val didSuffix = didSuffixFromDigest(operationDigest)

  val keys = List(
    DIDPublicKey(didSuffix, "master", KeyUsage.MasterKey, ECKeys.generateKeyPair().getPublic),
    DIDPublicKey(didSuffix, "issuing", KeyUsage.IssuingKey, ECKeys.generateKeyPair().getPublic),
    DIDPublicKey(didSuffix, "authentication", KeyUsage.AuthenticationKey, ECKeys.generateKeyPair().getPublic),
    DIDPublicKey(didSuffix, "communication", KeyUsage.CommunicationKey, ECKeys.generateKeyPair().getPublic)
  )

  "DIDDataRepository" should {
    "retrieve previously inserted DID data" in {
      val result = (for {
        _ <- didDataRepository.create(didSuffix, operationDigest, keys)
        did <- didDataRepository.findByDidSuffix(didSuffix)
      } yield did).value.futureValue.right.value

      result.didSuffix mustBe didSuffix
    }

    "return UnknownValueError when the DID is not found" in {
      val result = (for {
        _ <- didDataRepository.create(didSuffix, operationDigest, Nil)
        did <- didDataRepository.findByDidSuffix(didSuffixFromDigest(bytesGen(2)))
      } yield did).value.futureValue.left.value

      result must be(a[UnknownValueError])
    }

    "retrieve previously inserted DID key" in {
      val result = (for {
        _ <- didDataRepository.create(didSuffix, operationDigest, keys)
        key <- didDataRepository.findKey(didSuffix, "issuing")
      } yield key).value.futureValue.right.value

      result mustBe keys.tail.head
    }

    "return UnknownValueError when retrieving key for non-existing DID" in {
      val result = (for {
        _ <- didDataRepository.create(didSuffix, operationDigest, keys)
        key <- didDataRepository.findKey(didSuffixFromDigest(bytesGen(2)), "issuing")
      } yield key).value.futureValue.left.value

      result must be(a[UnknownValueError])
    }

    "return UnknownValueError when retrieving non-existing key" in {
      val result = (for {
        _ <- didDataRepository.create(didSuffix, operationDigest, keys.tail)
        key <- didDataRepository.findKey(didSuffix, "master")
      } yield key).value.futureValue.left.value

      result must be(a[UnknownValueError])

    }
  }

}
