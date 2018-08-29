package io.iohk.cef.crypto.low

import java.security.{KeyPairGenerator, PrivateKey, PublicKey, SecureRandom}
import javax.crypto.Cipher

import akka.util.ByteString

/**
 * The contract that all encryption / decryption implementations should follow.
 *
 * Designed for asymmetric encryption only.
 */
sealed trait CryptoAlgorithm {

  type DecryptError

  /**
    * Encrypts the provided `source` bytes with the provided `key`
    *
    * @param source     the bytes to be encrypted
    * @param key        the key used to encrypt `source`
    *
    * @return an encrypted version of the `source` bytes
    */
  def encrypt(source: ByteString, key: PublicKey): EncryptedData

  /**
    * Decrypts the provided `source` bytes with the provided `key`
    *
    * @param source  the encrypted string of bytes to be decrypted
    * @param key     the key to decrypt the `source`
    *
    * @return a decrypted version of the encrypted `source` if the `key` is able to decrypt it
    */
  def decrypt(source: EncryptedData, key: PrivateKey): Either[DecryptError, ByteString]

  /**
   * Generates a new asymmetric key pair.
   *
   * @return the generated key pair (public and private key)
   */
  def generateKeyPair: (PublicKey, PrivateKey)
}

/**
  * Companion object to CryptoAlgorithm, containing all the implemented `CryptoAlgorithm`
  */
object CryptoAlgorithm {

  class RSA(secureRandom: SecureRandom) extends CryptoAlgorithm {

    // TODO: use a meaningful error
    type DecryptError = Throwable

    override def encrypt(source: ByteString, key: PublicKey): EncryptedData = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.ENCRYPT_MODE, key)

      // TODO: Find a way to use buffers in order to not crash on huge inputs
      val data = cipher.doFinal(source.toArray)
      new EncryptedData(ByteString(data))
    }

    override def decrypt(source: EncryptedData, key: PrivateKey): Either[DecryptError, ByteString] = {
      try {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, key)

        // TODO: Find a way to use buffers in order to not crash on huge inputs
        val result = cipher.doFinal(source.value.toArray)
        Right(ByteString(result))
      } catch {
        case t: Throwable => Left(t)
      }
    }

    override def generateKeyPair: (PublicKey, PrivateKey) = {
      val generator = KeyPairGenerator.getInstance("RSA")
      generator.initialize(2048)
      val keyPair = generator.genKeyPair()

      (keyPair.getPublic, keyPair.getPrivate)
    }
  }
}
