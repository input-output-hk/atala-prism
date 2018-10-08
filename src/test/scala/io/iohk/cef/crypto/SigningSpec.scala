package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{EitherValues, MustMatchers, WordSpec}
import io.iohk.cef.test.ScalacheckExctensions

class SigningSpec extends WordSpec with MustMatchers with PropertyChecks with EitherValues with ScalacheckExctensions {

  case class User(name: String, age: Int)

  "generateSigningKeyPair" should {
    "always generate different key pairs" in {
      var publicKeys = Set.empty[ByteString]
      var privateKeys = Set.empty[ByteString]

      forAll { _: Int =>
        val result = generateSigningKeyPair()

        val pub = result.public.toByteString
        val priv = result.`private`.toByteString

        publicKeys.contains(pub) must be(false)
        privateKeys.contains(priv) must be(false)

        publicKeys += pub
        privateKeys += priv
      }
    }
  }

  "sign" should {
    "generate a signature for any ByteString input" in {
      forAll { input: ByteString =>
        val keys = generateSigningKeyPair()
        val result = sign(input, keys.`private`)

        result.toByteString mustNot be(empty)
      }
    }

    "generate a signature for any Entity input" in {
      forAll { (name: String, age: Int) =>
        val keys = generateSigningKeyPair()
        val entity = User(name, age)
        val result = sign(entity, keys.`private`)

        result.toByteString mustNot be(empty)
      }
    }
  }

  "isValidSignature" should {
    "verify the signature of any ByteString with the right key" in {
      forAll { input: ByteString =>
        val keys = generateSigningKeyPair()
        val signature = sign(input, keys.`private`)
        val result = isValidSignature(input, signature, keys.public)

        result must be(true)
      }
    }

    "fail to verify the signature of any ByteString with the wrong key" in {
      forAll { input: ByteString =>
        val keys = generateSigningKeyPair()
        val signature = sign(input, keys.`private`)

        forAll { _: Int =>
          val nested = generateSigningKeyPair().public
          val result = isValidSignature(input, signature, nested)
          result must be(false)
        }
      }
    }

    "verify the signature of any Entity with the right key" in {
      forAll { (name: String, age: Int) =>
        val keys = generateSigningKeyPair()
        val entity = User(name, age)
        val signature = sign(entity, keys.`private`)
        val result = isValidSignature(entity, signature, keys.public)

        result must be(true)
      }
    }

    "fail to verify the signature of any Entity with the wrong key" in {
      forAll { (name: String, age: Int) =>
        val keys: SigningKeyPair = generateSigningKeyPair()
        val entity = User(name, age)
        val signature = sign(entity, keys.`private`)

        forAll { _: Int =>
          val nested = generateSigningKeyPair().public
          val result = isValidSignature(entity, signature, nested)

          result must be(false)
        }
      }
    }
  }

  "Signature.decodeFrom" should {
    "decode valid signature" in {
      forAll { input: ByteString =>
        val keys = generateSigningKeyPair()
        val signature = sign(input, keys.`private`)
        val result = Signature.decodeFrom(signature.toByteString)

        result.right.value.toByteString.toArray mustNot be(empty)
      }
    }

    "fail to decode invalid signatures" in {
      pending

      forAll { bytes: ByteString =>
        val result = Signature.decodeFrom(bytes)
        val expected =
          SignatureDecodeError.DataExtractionError(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }

    "fail to decode signatures with unsupported algorithms" in {
      val algorithm = "SHA256withRSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      forAll { input: ByteString =>
        val keys = generateSigningKeyPair()
        val signature = sign(input, keys.`private`)

        val index = signature.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = signature.toByteString.updated(index, 'X'.toByte)

        val result = Signature.decodeFrom(corruptedBytes)
        val expected = SignatureDecodeError.UnsupportedAlgorithm("XHA256withRSA")

        result.left.value must be(expected)
      }
    }
  }

  "SigningPublicKey.decodeFrom" should {
    "decode valid public key" in {
      forAll { _: Int =>
        val key = generateSigningKeyPair().public
        val result = SigningPublicKey.decodeFrom(key.toByteString)

        result.right.value.toByteString.toArray mustNot be(empty)
      }
    }

    "fail to decode invalid public key" in {
      pending

      forAll { bytes: ByteString =>
        val result = SigningPublicKey.decodeFrom(bytes)
        val expected = SigningPublicKeyDecodeError.DataExtractionError(NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }

    "fail to decode public keys with unsupported algorithms" in {
      val algorithm = "SHA256withRSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      forAll { _: Int =>
        val key = generateSigningKeyPair().public

        val index = key.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = key.toByteString.updated(index, 'X'.toByte)

        val result = SigningPublicKey.decodeFrom(corruptedBytes)
        val expected = SigningPublicKeyDecodeError.UnsupportedAlgorithm("XHA256withRSA")

        result.left.value must be(expected)
      }
    }
  }

  "SigningPrivateKey.decodeFrom" should {
    "decode valid private key" in {
      forAll { _: Int =>
        val key = generateSigningKeyPair().`private`
        val result = SigningPrivateKey.decodeFrom(key.toByteString)

        result.right.value.toByteString.toArray mustNot be(empty)
      }
    }

    "fail to decode invalid private key" in {
      pending

      forAll { bytes: ByteString =>
        val result = SigningPrivateKey.decodeFrom(bytes)
        val expected = SigningPrivateKeyDecodeError.DataExtractionError(NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }

    "fail to decode private keys with unsupported algorithms" in {
      val algorithm = "SHA256withRSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      forAll { _: Int =>
        val key = generateSigningKeyPair().`private`

        val index = key.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = key.toByteString.updated(index, 'X'.toByte)

        val result = SigningPrivateKey.decodeFrom(corruptedBytes)
        val expected = SigningPrivateKeyDecodeError.UnsupportedAlgorithm("XHA256withRSA")

        result.left.value must be(expected)
      }
    }
  }
}
