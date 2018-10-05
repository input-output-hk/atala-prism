package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.crypto.encoding.TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS
import io.iohk.cef.network.encoding.nio._
import org.scalatest.MustMatchers._
import org.scalatest.prop.PropertyChecks._
import org.scalatest.EitherValues._
import org.scalatest.WordSpec
import io.iohk.cef.test.ScalacheckExtensions._

class EncryptionSpec extends WordSpec {

  case class User(name: String, age: Int)
  private val keys = generateEncryptionKeyPair()

  "generateEncryptionKeyPair" should {
    "generate different set of key pair each time" in {
      val key1 = generateEncryptionKeyPair()
      val key2 = generateEncryptionKeyPair()
      key1 != key2 mustBe true
    }
  }

  "encrypt" should {
    "encrypt any ByteString input" in {
      forAll { input: ByteString =>
        val result = encrypt(input, keys.public)

        result.toByteString mustNot be(empty)
      }
    }

    "encrypt any Entity input" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val result = encrypt(entity, keys.public)

        result.toByteString mustNot be(empty)
      }
    }
  }

  "encryption / decryption with valid  keypair" should {
    "encrypt ByteString input and decrypt" in {
      forAll { input: ByteString =>
        val encrypted = encrypt(input, keys.public)
        val decrypted = decrypt(encrypted, keys.`private`)
        decrypted.right.value mustBe input
      }
    }
    "encrypt any Entity input and decrypt" in {
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val encrypted = encrypt(entity, keys.public)
        val decrypted = decrypt[User](encrypted, keys.`private`)
        decrypted.right.value mustBe entity
      }
    }
  }

  "Decrypting a encrypted input with wrong key" should {
    "fail to decrypt given ByteString " in {
      val privateKey = generateEncryptionKeyPair().`private`
      forAll { input: ByteString =>
        val encrypted = encrypt(input, keys.public)
        forAll { _: Int =>
          val decrypted = decrypt(encrypted, privateKey)
          decrypted.left.value.isInstanceOf[DecryptError.UnderlayingDecryptionError] must be(true)
        }
      }
    }
    "fail to decrypt given Entity" in {
      val privateKey = generateEncryptionKeyPair().`private`
      forAll { (name: String, age: Int) =>
        val entity = User(name, age)
        val encrypted = encrypt(entity, keys.public)
        forAll { _: Int =>
          val decrypted = decrypt[User](encrypted, privateKey)
          decrypted.left.value.isInstanceOf[DecryptError.UnderlayingDecryptionError] must be(true)
        }
      }
    }
  }

  "EncryptedData" should {

    import io.iohk.cef.crypto.EncryptedDataDecodeError.DataExtractionError
    "decode valid data" in {
      forAll { input: ByteString =>
        val encrypted = encrypt(input, keys.public)
        val result = EncryptedData.decodeFrom(encrypted.toByteString)

        result.right.value.toByteString must be(encrypted.toByteString)
      }
    }

    "fail to decode invalid data" in {
      pending
      forAll { bytes: ByteString =>
        val result = EncryptedData.decodeFrom(bytes)
        val expected = DataExtractionError(NioDecoderFailedToDecodeTBS)
        result.left.value must be(expected)
      }
    }

    "fail to decode data with unsupported algorithms" in {
      val algorithm = "RSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      forAll { input: ByteString =>
        val encrypted = encrypt(input, keys.public)

        val index = encrypted.toByteString.indexOfSlice(algorithm)
        val corruptedBytes = encrypted.toByteString.updated(index, 'X'.toByte)

        val result = EncryptedData.decodeFrom(corruptedBytes)
        val expected = EncryptedDataDecodeError.UnsupportedAlgorithm("XSA")

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
      pending

      forAll { bytes: ByteString =>
        val result = EncryptionPublicKey.decodeFrom(bytes)
        val expected = EncryptionPublicKeyDecodeError.DataExtractionError(NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }

    "fail to decode public keys with unsupported algorithms" in {
      val algorithm = "RSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray
      val key = keys.public
      val index = key.toByteString.indexOfSlice(algorithm)
      val corruptedBytes = key.toByteString.updated(index, 'X'.toByte)
      val result = EncryptionPublicKey.decodeFrom(corruptedBytes)
      val expected = EncryptionPublicKeyDecodeError.UnsupportedAlgorithm("XSA")

      result.left.value must be(expected)
    }

    "decode valid private key" in {
      val key = keys.`private`
      val result = EncryptionPrivateKey.decodeFrom(key.toByteString)

      result.right.value.toByteString must be(key.toByteString)
    }

    "fail to decode invalid private key" in {
      pending

      forAll { bytes: ByteString =>
        val result = EncryptionPrivateKey.decodeFrom(bytes)
        val expected = EncryptionPrivateKeyDecodeError.DataExtractionError(NioDecoderFailedToDecodeTBS)

        result.left.value must be(expected)
      }
    }
    "fail to decode private keys with unsupported algorithms" in {
      val algorithm = "RSA".flatMap(_.toByte :: 0.toByte :: Nil).toArray

      val key = keys.`private`

      val index = key.toByteString.indexOfSlice(algorithm)
      val corruptedBytes = key.toByteString.updated(index, 'X'.toByte)

      val result = EncryptionPrivateKey.decodeFrom(corruptedBytes)
      val expected = EncryptionPrivateKeyDecodeError.UnsupportedAlgorithm("XSA")

      result.left.value must be(expected)

    }
  }

}
