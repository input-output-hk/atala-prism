package io.iohk.cef.crypto
package encryption
package algorithms

import io.iohk.cef.crypto.KeyDecodingError
import java.security.{SecureRandom, KeyPairGenerator, KeyFactory}
import akka.util.ByteString
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher

class RSA(secureRandom: SecureRandom) extends EncryptionAlgorithm {

  type PublicKey = java.security.PublicKey
  type PrivateKey = java.security.PrivateKey

  def encrypt(source: ByteString, key: PublicKey): EncryptedBytes = {
    val cipher = Cipher.getInstance("RSA")
    cipher.init(Cipher.ENCRYPT_MODE, key)

    // TODO: Find a way to use buffers in order to not crash on huge inputs
    val data = cipher.doFinal(source.toArray)
    EncryptedBytes(ByteString(data))
  }

  def decrypt(source: EncryptedBytes, key: PrivateKey): Either[DecryptError, ByteString] = {
    try {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.DECRYPT_MODE, key)

      // TODO: Find a way to use buffers in order to not crash on huge inputs
      val result = cipher.doFinal(source.bytes.toArray)
      Right(ByteString(result))
    } catch {
      case t: Throwable => Left(DecryptError.UnderlayingImplementationError(t.getMessage))
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
