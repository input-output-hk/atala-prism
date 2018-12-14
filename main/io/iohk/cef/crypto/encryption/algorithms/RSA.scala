package io.iohk.cef.crypto
package encryption
package algorithms

import io.iohk.cef.crypto.encoding.KeyDecodingError
import java.security.{SecureRandom, KeyPairGenerator, KeyFactory}
import akka.util.ByteString
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher

class RSA(secureRandom: SecureRandom) extends EncryptionAlgorithm {

  private val BLOCK_SIZE = 245
  private val DECRYPT_BLOCK_SIZE = 256

  type PublicKey = java.security.PublicKey
  type PrivateKey = java.security.PrivateKey

  def encrypt(source: ByteString, key: PublicKey): EncryptedBytes = {
    if (source.size == 0) {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.ENCRYPT_MODE, key)

      val result = cipher.doFinal(source.toArray)

      EncryptedBytes(ByteString(result))
    } else {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.ENCRYPT_MODE, key)

      val l = source.size
      var pointer = 0
      val builder = ByteString.newBuilder
      while (pointer < l) {
        val delta = BLOCK_SIZE min (l - pointer)
        val targetLength = cipher.getOutputSize(delta)
        val fragment = new Array[Byte](targetLength)
        cipher.doFinal(source.drop(pointer).take(delta).toArray, 0, delta, fragment, 0)
        pointer += BLOCK_SIZE
        builder ++= fragment
      }

      EncryptedBytes(builder.result)
    }
  }

  def decrypt(source: EncryptedBytes, key: PrivateKey): Either[DecryptError, ByteString] = {
    try {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.DECRYPT_MODE, key)

      val l = source.bytes.size
      var pointer = 0
      val builder = ByteString.newBuilder
      while (pointer < l) {
        val delta = DECRYPT_BLOCK_SIZE min (l - pointer)
        val fragment = cipher.doFinal(source.bytes.drop(pointer).take(delta).toArray, 0, DECRYPT_BLOCK_SIZE)
        pointer += DECRYPT_BLOCK_SIZE
        builder ++= fragment
      }

      Right(builder.result)
    } catch {
      case t: Throwable =>
        Left(DecryptError.UnderlayingImplementationError(t.getMessage))
    }
  }

  override def generateKeyPair(): (PublicKey, PrivateKey) = {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048)
    val keyPair = generator.genKeyPair()

    (keyPair.getPublic, keyPair.getPrivate)
  }

  override def encodePublicKey(key: PublicKey): PublicKeyBytes =
    PublicKeyBytes(ByteString(key.getEncoded))

  override def encodePrivateKey(key: PrivateKey): PrivateKeyBytes =
    PrivateKeyBytes(ByteString(key.getEncoded))

  override def decodePublicKey(bytes: PublicKeyBytes): Either[KeyDecodingError, PublicKey] = {
    try {
      val publicKeySpec = new X509EncodedKeySpec(bytes.bytes.toArray)
      val keyFactory = KeyFactory.getInstance("RSA")
      Right(keyFactory.generatePublic(publicKeySpec))
    } catch {
      case t: Throwable =>
        Left(KeyDecodingError.UnderlayingImplementationError(t.getMessage))
    }
  }

  override def decodePrivateKey(bytes: PrivateKeyBytes): Either[KeyDecodingError, PrivateKey] = {
    try {
      val privateKeySpec = new PKCS8EncodedKeySpec(bytes.bytes.toArray)
      val keyFactory = KeyFactory.getInstance("RSA")
      Right(keyFactory.generatePrivate(privateKeySpec))
    } catch {
      case t: Throwable =>
        Left(KeyDecodingError.UnderlayingImplementationError(t.getMessage))
    }
  }

}
