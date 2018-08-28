package io.iohk.cef.crypto.low

import java.security.Security

import akka.util.ByteString
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
 * The contract that all encryption / decryption implementations should follow.
 *
 * Designed for asymmetric encryption only.
 */
sealed trait CryptoAlgorithm {

  type PublicKey
  type PrivateKey

  type DecryptError

  /**
    * Encrypts the provided `source` bytes with the provided `key`
    *
    * @param source     the bytes to be encrypted
    * @param key        the key used to encrypt `source`
    *
    * @return an encrypted version of the `source` bytes
    */
  def encrypt(source: ByteString, key: PublicKey): ByteString

  /**
    * Decrypts the provided `source` bytes with the provided `key`
    *
    * @param source  the encrypted string of bytes to be decrypted
    * @param key     the key to decrypt the `source`
    *
    * @return a decrypted version of the encrypted `source` if the `key` is able to decrypt it
    */
  def decrypt(source: ByteString, key: PrivateKey): Either[DecryptError, ByteString]

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

  Security.addProvider(new BouncyCastleProvider())

  case class RSA(secureRandom: java.security.SecureRandom) extends CryptoAlgorithm {
    import java.math.BigInteger

    import org.bouncycastle.crypto.engines.RSAEngine
    import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
    import org.bouncycastle.crypto.params.AsymmetricKeyParameter
    import org.bouncycastle.crypto.params.RSAKeyGenerationParameters

    // TODO: use specific type for each key
    type PublicKey = AsymmetricKeyParameter
    type PrivateKey = AsymmetricKeyParameter

    // TODO: use a meaningful error
    type DecryptError = Throwable

    override def encrypt(source: ByteString,key: PublicKey): ByteString = {
      val engine = new RSAEngine()
      engine.init(true, key)

      // TODO: This could be problematic if source is huge. Should be addressed in a safe way
      val data = source.toArray
      ByteString(engine.processBlock(data, 0, data.length))
    }

    override def decrypt(source: ByteString, key: PrivateKey): Either[DecryptError, ByteString] = {
      try {
        val engine = new RSAEngine()
        engine.init(false, key)

        // TODO: This could be problematic if source is huge. Should be addressed in safe way
        val encryptedBytes = source.toArray
        val result = ByteString(engine.processBlock(encryptedBytes, 0, encryptedBytes.length))
        Right(result)
      }
      catch {
        case t: Throwable => Left(t)
      }
    }

    override def generateKeyPair: (PublicKey, PrivateKey) = {
      val generator  = new RSAKeyPairGenerator()

      // TODO: Verify that this is secure
      val parameters = new RSAKeyGenerationParameters (
                         new BigInteger("10001", 16),//publicExponent
                         secureRandom,
                         4096,//strength
                         80//certainty
                       )
      generator.init(parameters)
      val pair = generator.generateKeyPair()
      (pair.getPublic, pair.getPrivate)
    }
  }

}



