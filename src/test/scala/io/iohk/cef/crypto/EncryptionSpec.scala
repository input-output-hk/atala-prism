package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS
import io.iohk.cef.network.encoding.nio._
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{EitherValues, MustMatchers, WordSpec}

class EncryptionSpec extends WordSpec with MustMatchers with PropertyChecks with EitherValues {

  case class User(name: String, age: Int)

  "generateEncryptionKeyPair" should {
    "always generate different key pairs" in {
      var publicKeys = Set.empty[ByteString]
      var privateKeys = Set.empty[ByteString]

      forAll { _: Int =>
        val result = generateEncryptionKeyPair()

        val pub = result.public.toByteString
        val priv = result.`private`.toByteString

        publicKeys.contains(pub) must be(false)
        privateKeys.contains(priv) must be(false)

        publicKeys += pub
        privateKeys += priv
      }
    }
  }

  "encryptBytes" should {
    "encrypt any input" in {
      forAll { input: Array[Byte] =>
        val keys = generateEncryptionKeyPair()
        val result = encryptBytes(ByteString(input), keys.public)

        result.toByteString mustNot be(empty)
      }
    }
  }

  "encryptEntity" should {
    "encrypt any input" in {
      forAll { (name: String, age: Int) =>
        val keys = generateEncryptionKeyPair()
        val entity = User(name, age)
        val result = encryptEntity(entity, keys.public)

        result.toByteString mustNot be(empty)
      }
    }
  }

  "decryptBytes" should {
    "decrypt with the right key" in {
      forAll { input: Array[Byte] =>
        val keys = generateEncryptionKeyPair()
        val encrypted = encryptBytes(ByteString(input), keys.public)
        val result = decryptBytes(encrypted, keys.`private`)

        result.right.value.toArray must be(input)
      }
    }

    "fail to decrypt with the wrong key" in {
      forAll { input: Array[Byte] =>
        val keys = generateEncryptionKeyPair()
        val encrypted = encryptBytes(ByteString(input), keys.public)

        forAll { _: Int =>
          val nested = generateEncryptionKeyPair().`private`
          val result = decryptBytes(encrypted, nested)

          result.left.value.isInstanceOf[DecryptError.UnderlayingDecryptionError] must be(true)
        }
      }
    }
  }

  "decryptEntity" should {
    "decrypt with the right key" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val keys = generateEncryptionKeyPair()
        val encrypted = encryptEntity(entity, keys.public)
        val result = decryptEntity[User](encrypted, keys.`private`)

        result.right.value must be(entity)
      }
    }

    "fail to decrypt with the wrong key" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val keys = generateEncryptionKeyPair()
        val encrypted = encryptEntity(entity, keys.public)

        forAll { _: Int =>
          val nested = generateEncryptionKeyPair().`private`
          val result = decryptEntity[User](encrypted, nested)

          result.left.value.isInstanceOf[DecryptError.UnderlayingDecryptionError] must be(true)
        }
      }
    }
  }

  "EncryptedData.decodeFrom" should {
    "decode valid data" in {
      forAll { input: Array[Byte] =>
        val keys = generateEncryptionKeyPair()
        val encrypted = encryptBytes(ByteString(input), keys.public)
        val result = EncryptedData.decodeFrom(encrypted.toByteString)

        result.right.value.toByteString must be(encrypted.toByteString)
      }
    }

    "fail to decode invalid data" in {
      pending

      forAll { bytes: Array[Byte] =>
        val result = EncryptedData.decodeFrom(ByteString(bytes))
        val expected =
          EncryptedDataDecodeError.DataExtractionError(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }

    "fail to decode signatures with unsupported algorithms" in {
      val algorithm = "RSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      forAll { input: Array[Byte] =>
        val keys = generateEncryptionKeyPair()
        val encrypted = encryptBytes(ByteString(input), keys.public)

        val index = encrypted.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = encrypted.toByteString.toArray
        corruptedBytes(index) = 'X'.toByte

        val result = EncryptedData.decodeFrom(ByteString(corruptedBytes))
        val expected = EncryptedDataDecodeError.UnsupportedAlgorithm("XSA")

        result.left.value must be(expected)
      }
    }
  }

  "EncryptionPublicKey.decodeFrom" should {
    "decode valid public key" in {
      forAll { _: Int =>
        val key = generateEncryptionKeyPair().public
        val result = EncryptionPublicKey.decodeFrom(key.toByteString)

        result.right.value.toByteString must be(key.toByteString)
      }
    }

    "fail to decode invalid public key" in {
      pending

      forAll { bytes: Array[Byte] =>
        val result = EncryptionPublicKey.decodeFrom(ByteString(bytes))
        val expected = EncryptionPublicKeyDecodeError.DataExtractionError(NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }

    "fail to decode public keys with unsupported algorithms" in {
      val algorithm = "RSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      forAll { _: Int =>
        val key = generateEncryptionKeyPair().public

        val index = key.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = key.toByteString.toArray
        corruptedBytes(index) = 'X'.toByte

        val result = EncryptionPublicKey.decodeFrom(ByteString(corruptedBytes))
        val expected = EncryptionPublicKeyDecodeError.UnsupportedAlgorithm("XSA")

        result.left.value must be(expected)
      }
    }
  }

  "EncryptionPrivateKey.decodeFrom" should {
    "decode valid private key" in {
      forAll { _: Int =>
        val key = generateEncryptionKeyPair().`private`
        val result = EncryptionPrivateKey.decodeFrom(key.toByteString)

        result.right.value.toByteString must be(key.toByteString)
      }
    }

    "fail to decode invalid private key" in {
      pending

      forAll { bytes: Array[Byte] =>
        val result = EncryptionPrivateKey.decodeFrom(ByteString(bytes))
        val expected = EncryptionPrivateKeyDecodeError.DataExtractionError(NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }

    "fail to decode private keys with unsupported algorithms" in {
      val algorithm = "RSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      forAll { _: Int =>
        val key = generateEncryptionKeyPair().`private`

        val index = key.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = key.toByteString.toArray
        corruptedBytes(index) = 'X'.toByte

        val result = EncryptionPrivateKey.decodeFrom(ByteString(corruptedBytes))
        val expected = EncryptionPrivateKeyDecodeError.UnsupportedAlgorithm("XSA")

        result.left.value must be(expected)
      }
    }
  }
}
