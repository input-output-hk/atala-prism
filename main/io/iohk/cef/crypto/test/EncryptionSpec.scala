package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS
import org.scalatest.MustMatchers._
import org.scalatest.prop.PropertyChecks._
import org.scalatest.EitherValues._
import org.scalatest.WordSpec
import io.iohk.cef.test.ScalacheckExtensions._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto.encoding._

class EncryptionSpec extends WordSpec {

  case class User(name: String, age: Int)
  private val keys = generateEncryptionKeyPair()
  private val moreKeys = generateEncryptionKeyPair()

  "generateEncryptionKeyPair" should {
    "generate a different key pair each time" in {
      keys must not be moreKeys
    }
  }

  "encrypt" should {
    "encrypt any Entity input" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val result = encrypt(entity, keys.public)

        result.toByteString mustNot be(empty)
      }
    }
  }

  "encryption / decryption with valid  keypair" should {
    "encrypt any Entity input and decrypt" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val encrypted = encrypt(entity, keys.public)
        val decrypted = decrypt[User](encrypted, keys.`private`)
        decrypted.right.value mustBe entity
      }
    }
  }

  "Decrypting an encrypted input with wrong key" should {
    "fail to decrypt given Entity" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val encrypted = encrypt(entity, keys.public)
        val decrypted = decrypt[User](encrypted, moreKeys.`private`)
        decrypted.left.value.isInstanceOf[DecryptError.UnderlayingDecryptionError] must be(true)
      }
    }
  }

  "EncryptedData" should {

    "decode valid data" in {
      forAll { input: ByteString =>
        val encrypted = encrypt(input, keys.public)
        val result = EncryptedData.decodeFrom(encrypted.toByteString)

        result.right.value.toByteString must be(encrypted.toByteString)
      }
    }

    "fail to decode invalid data" in {
      forAll { bytes: ByteString =>
        val result = EncryptedData.decodeFrom(bytes)
        val expected = DecodeError.DataExtractionError(NioDecoderFailedToDecodeTBS)
        result.left.value must be(expected)
      }
    }

    "fail to decode data with unsupported algorithms" in {
      val algorithm = "RSA".getBytes("UTF-8")
      forAll { input: ByteString =>
        val encrypted = encrypt(input, keys.public)

        val index = encrypted.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = encrypted.toByteString.updated(index, 'X'.toByte)

        val result = EncryptedData.decodeFrom(corruptedBytes)
        val expected = DecodeError.UnsupportedAlgorithm("XSA")

        result.left.value must be(expected)
      }
    }
  }

  "Encryption" should {
    "decode valid public key" in {
      val key = keys.public
      val result = EncryptionPublicKey.decodeFrom(key.toByteString)
      result.right.value.toByteString must be(key.toByteString)
    }

    "fail to decode invalid public key" in {

      forAll { bytes: ByteString =>
        type T = KeyDecodeError[EncryptionPublicKey]
        val result = EncryptionPublicKey.decodeFrom(bytes)
        val expected: T = KeyDecodeError.DataExtractionError(NioDecoderFailedToDecodeTBS)
        val response: T = result.left.value

        response must ===(expected)
      }
    }

    "fail to decode public keys with unsupported algorithms" in {
      val algorithm = "RSA".getBytes("UTF-8")
      val key = keys.public
      val index = key.toByteString.indexOfSlice(algorithm)
      val corruptedBytes = key.toByteString.updated(index, 'X'.toByte)
      val result = EncryptionPublicKey.decodeFrom(corruptedBytes)
      val expected = KeyDecodeError.UnsupportedAlgorithm("XSA")

      result.left.value must be(expected)
    }

    "decode valid private key" in {
      val key = keys.`private`
      val result = EncryptionPrivateKey.decodeFrom(key.toByteString)

      result.right.value.toByteString must be(key.toByteString)
    }

    "fail to decode invalid private key" in {

      forAll { bytes: ByteString =>
        val result = EncryptionPrivateKey.decodeFrom(bytes)
        val expected = KeyDecodeError.DataExtractionError(NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }
    "fail to decode private keys with unsupported algorithms" in {
      val algorithm = "RSA".getBytes("UTF-8")

      val key = keys.`private`

      val index = key.toByteString.indexOfSlice(algorithm)
      val corruptedBytes = key.toByteString.updated(index, 'X'.toByte)

      val result = EncryptionPrivateKey.decodeFrom(corruptedBytes)
      val expected = KeyDecodeError.UnsupportedAlgorithm("XSA")

      result.left.value must be(expected)

    }
  }

}
