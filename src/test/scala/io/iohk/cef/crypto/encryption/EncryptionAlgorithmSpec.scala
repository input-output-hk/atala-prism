package io.iohk.cef.crypto.encryption

import akka.util.ByteString
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import io.iohk.cef.builder.SecureRandomBuilder
import io.iohk.cef.test.ExtraScalacheckGenerators
import io.iohk.cef.crypto.KeyDecodingError

class EncryptionAlogorithmSpec
    extends FlatSpec
    with PropertyChecks
    with SecureRandomBuilder
    with ExtraScalacheckGenerators {

  val encryptionCollection: EncryptionAlgorithmsCollection =
    EncryptionAlgorithmsCollection(secureRandom)

  val all = encryptionCollection.EncryptionAlgorithmType.values

  all.foreach { `type` =>
    val description = `type`.algorithmIdentifier
    val algorithm = `type`.algorithm

    // Here the tests are run
    sharedTest(description, algorithm)
  }

  protected def sharedTest(algorithmDescription: String, algorithm: EncryptionAlgorithm): Unit = {

    val MAX = 30
    def eachTime[T](f: => T): Unit = (1 to MAX).foreach { _ =>
      f
    }

    s"$algorithmDescription.generateKeyPair()" should "always generate different key pairs" in {
      (1 to MAX).foldLeft((Set.empty[ByteString], Set.empty[ByteString])) {
        case ((publicKeys, privateKeys), _) =>
          val (pubKey, privKey) = algorithm.generateKeyPair()

          val pubBytes = algorithm.encodePublicKey(pubKey).bytes
          val privBytes = algorithm.encodePrivateKey(privKey).bytes

          publicKeys.contains(pubBytes) should be(false)
          privateKeys.contains(privBytes) should be(false)

          (publicKeys + pubBytes, privateKeys + privBytes)
      }
    }

    s"$algorithmDescription.encrypt" should "encrypt any input" in {
      val (pubKey, privKey) = algorithm.generateKeyPair()
      forAll { input: ByteString =>
        val result = algorithm.encrypt(input, pubKey)

        result.bytes shouldNot be(empty)
      }
    }

    s"$algorithmDescription.decrypt" should "decrypt with the right key" in {
      val (pubKey, privKey) = algorithm.generateKeyPair()
      forAll { input: ByteString =>
        val encrypted = algorithm.encrypt(input, pubKey)
        val Right(result) = algorithm.decrypt(encrypted, privKey)

        result should be(input)
      }
    }

    it should "fail to decrypt with the wrong key" in {
      val (pubKey, privKey) = algorithm.generateKeyPair()
      forAll { input: ByteString =>
        val encrypted = algorithm.encrypt(input, pubKey)

        eachTime {
          val (_, wrongPrivKey) = algorithm.generateKeyPair()
          val Left(result) = algorithm.decrypt(encrypted, wrongPrivKey)

          result.isInstanceOf[DecryptError.UnderlayingImplementationError] should be(true)
        }
      }
    }

    s"$algorithmDescription.decodePublicKey" should "decode a valid public key" in {
      eachTime {
        val (publicKey, _) = algorithm.generateKeyPair()
        val encoded = algorithm.encodePublicKey(publicKey)
        val Right(result) = algorithm.decodePublicKey(encoded)

        algorithm.encodePublicKey(result) should be(encoded)
      }
    }

    it should "fail to decode invalid public key" in {
      forAll { bytes: ByteString =>
        val Left(result) = algorithm.decodePublicKey(PublicKeyBytes(bytes))

        result.isInstanceOf[KeyDecodingError.UnderlayingImplementationError] should be(true)
      }
    }

    s"$algorithmDescription.decodePrivateKey" should "decode a valid private key" in {
      eachTime {
        val (_, privateKey) = algorithm.generateKeyPair()
        val encoded = algorithm.encodePrivateKey(privateKey)
        val Right(result) = algorithm.decodePrivateKey(encoded)

        algorithm.encodePrivateKey(result) should be(encoded)
      }
    }

    it should "fail to decode invalid private key" in {
      forAll { bytes: ByteString =>
        val Left(result) = algorithm.decodePrivateKey(PrivateKeyBytes(bytes))

        result.isInstanceOf[KeyDecodingError.UnderlayingImplementationError] should be(true)
      }
    }

  }

}
