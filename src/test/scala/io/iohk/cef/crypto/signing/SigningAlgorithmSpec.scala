package io.iohk.cef.crypto.signing

import akka.util.ByteString
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import io.iohk.cef.builder.SecureRandomBuilder
import io.iohk.cef.test.ScalacheckExctensions
import io.iohk.cef.crypto.KeyDecodingError

class SigningAlogorithmSpec extends FlatSpec with PropertyChecks with SecureRandomBuilder with ScalacheckExctensions {

  val signingCollection: SigningAlgorithmsCollection =
    SigningAlgorithmsCollection(secureRandom)

  val all = signingCollection.SigningAlgorithmType.values

  all.foreach { `type` =>
    val description = `type`.algorithmIdentifier
    val algorithm = `type`.algorithm

    // Here the tests are run
    sharedTest(description, algorithm)
  }

  protected def sharedTest(algorithmDescription: String, algorithm: SigningAlgorithm): Unit = {

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

    s"$algorithmDescription.sign" should "generate a signature for any input" in {
      forAll { input: ByteString =>
        val (_, privateKey) = algorithm.generateKeyPair()
        val result = algorithm.sign(input, privateKey)

        result.bytes shouldNot be(empty)
      }
    }

    s"$algorithmDescription.isSignatureValid" should "verify the signature with the right key" in {
      forAll { input: ByteString =>
        val (publicKey, privateKey) = algorithm.generateKeyPair()
        val signature = algorithm.sign(input, privateKey)
        val result = algorithm.isSignatureValid(signature, input, publicKey)

        result should be(true)
      }
    }

    it should "fail to verify the signature with the wrong key" in {
      forAll { input: ByteString =>
        val (publicKey, privateKey) = algorithm.generateKeyPair()
        val signature = algorithm.sign(input, privateKey)

        eachTime {
          val (wrongPublicKey, wrongPrivateKey) = algorithm.generateKeyPair()
          val result = algorithm.isSignatureValid(signature, input, wrongPublicKey)
          result should be(false)
        }
      }
    }

    it should "fail to verify the wrong signature" in {
      forAll { input: ByteString =>
        val (publicKey, _) = algorithm.generateKeyPair()

        forAll { wrongSignatureBytes: ByteString =>
          val wrongSignature = SignatureBytes(wrongSignatureBytes)
          val result = algorithm.isSignatureValid(wrongSignature, input, publicKey)
          result should be(false)
        }
      }
    }

  }

}
