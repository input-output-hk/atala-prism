package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{EitherValues, MustMatchers, WordSpec}
import io.iohk.cef.test.ScalacheckExctensions

class EncryptionSpec
    extends WordSpec
    with MustMatchers
    with PropertyChecks
    with EitherValues
    with ScalacheckExctensions {

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

  "encrypt" should {
    "encrypt any ByteString input" in {
      forAll { input: ByteString =>
        val keys = generateEncryptionKeyPair()
        val result = encrypt(input, keys.public)

        result.toByteString mustNot be(empty)
      }
    }

    "encrypt any Entity input" in {
      forAll { (name: String, age: Int) =>
        val keys = generateEncryptionKeyPair()
        val entity = User(name, age)
        val result = encrypt(entity, keys.public)

        result.toByteString mustNot be(empty)
      }
    }
  }

  "decrypt" should {
    "decrypt ByteStrings with the right key" in {
      forAll { input: ByteString =>
        val keys = generateEncryptionKeyPair()
        val encrypted = encrypt(input, keys.public)
        val result = decrypt(encrypted, keys.`private`)

        result.right.value must be(input)
      }
    }

    "fail to decrypt ByteStrings with the wrong key" in {
      forAll { input: ByteString =>
        val keys = generateEncryptionKeyPair()
        val encrypted = encrypt(input, keys.public)

        forAll { _: Int =>
          val nested = generateEncryptionKeyPair().`private`
          val result = decrypt(encrypted, nested)

          result.left.value.isInstanceOf[DecryptError.UnderlayingDecryptionError] must be(true)
        }
      }
    }

    "decrypt Entities with the right key" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val keys = generateEncryptionKeyPair()
        val encrypted = encrypt(entity, keys.public)
        val result = decrypt[User](encrypted, keys.`private`)

        result.right.value must be(entity)
      }
    }

    "fail to decrypt Entities with the wrong key" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val keys = generateEncryptionKeyPair()
        val encrypted = encrypt(entity, keys.public)

        forAll { _: Int =>
          val nested = generateEncryptionKeyPair().`private`
          val result = decrypt[User](encrypted, nested)

          result.left.value.isInstanceOf[DecryptError.UnderlayingDecryptionError] must be(true)
        }
      }
    }
  }

  "EncryptedData.decodeFrom" should {
    "decode valid data" in {
      forAll { input: ByteString =>
        val keys = generateEncryptionKeyPair()
        val encrypted = encrypt(input, keys.public)
        val result = EncryptedData.decodeFrom(encrypted.toByteString)

        result.right.value.toByteString must be(encrypted.toByteString)
      }
    }

    "fail to decode invalid data" in {
      pending

      forAll { bytes: ByteString =>
        val result = EncryptedData.decodeFrom(bytes)
        val expected =
          DecodeError.DataExtractionError[EncryptedData](TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }

    "fail to decode data with unsupported algorithms" in {
      val algorithm = "RSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      forAll { input: ByteString =>
        val keys = generateEncryptionKeyPair()
        val encrypted = encrypt(input, keys.public)

        val index = encrypted.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = encrypted.toByteString.updated(index, 'X'.toByte)

        val result = EncryptedData.decodeFrom(corruptedBytes)
        val expected = DecodeError.UnsupportedAlgorithm[EncryptedData]("XSA")

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

      forAll { bytes: ByteString =>
        val result = EncryptionPublicKey.decodeFrom(bytes)
        val expected = KeyDecodeError.DataExtractionError[EncryptionPublicKey](NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }

    "fail to decode public keys with unsupported algorithms" in {
      val algorithm = "RSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      forAll { _: Int =>
        val key = generateEncryptionKeyPair().public

        val index = key.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = key.toByteString.updated(index, 'X'.toByte)

        val result = EncryptionPublicKey.decodeFrom(corruptedBytes)
        val expected = KeyDecodeError.UnsupportedAlgorithm[EncryptionPublicKey]("XSA")

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

      forAll { bytes: ByteString =>
        val result = EncryptionPrivateKey.decodeFrom(bytes)
        val expected = KeyDecodeError.DataExtractionError[EncryptionPrivateKey](NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }

    "fail to decode private keys with unsupported algorithms" in {
      val algorithm = "RSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      forAll { _: Int =>
        val key = generateEncryptionKeyPair().`private`

        val index = key.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = key.toByteString.updated(index, 'X'.toByte)

        val result = EncryptionPrivateKey.decodeFrom(corruptedBytes)
        val expected = KeyDecodeError.UnsupportedAlgorithm[EncryptionPrivateKey]("XSA")

        result.left.value must be(expected)
      }
    }
  }
}
