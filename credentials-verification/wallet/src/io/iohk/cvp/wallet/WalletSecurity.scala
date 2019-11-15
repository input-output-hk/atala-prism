package io.iohk.cvp.wallet

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import scala.util.control.NonFatal

class WalletSecurity(keySalt: String) {

  private[this] val Encoding = "UTF-8"
  private[this] val KeySalt = keySalt
  private[this] val AES = "AES"
  private[this] val HashingAlgorithm = "PBKDF2WithHmacSHA512"
  private[this] val HashingIterations = 10000
  private[this] val KeySizeBits = 128
  private[this] val Algorithm = AES + "/ECB/PKCS5Padding"

  private[wallet] def generateSecretKey(passphrase: String): SecretKeySpec = {
    import java.security.NoSuchAlgorithmException
    import java.security.spec.InvalidKeySpecException

    import javax.crypto.SecretKeyFactory
    import javax.crypto.spec.PBEKeySpec

    def hashPassword(password: Array[Char], salt: Array[Byte], iterations: Int, keyLength: Int): Array[Byte] =
      try {
        val keyFactory = SecretKeyFactory.getInstance(HashingAlgorithm)
        val keySpec = new PBEKeySpec(password, salt, iterations, keyLength)
        val key = keyFactory.generateSecret(keySpec)
        key.getEncoded
      } catch {
        case e @ (_: NoSuchAlgorithmException | _: InvalidKeySpecException) =>
          throw new RuntimeException("Password hashing error", e)
      }

    new SecretKeySpec(
      hashPassword(passphrase.toCharArray, KeySalt.getBytes(Encoding), HashingIterations, KeySizeBits),
      AES
    )
  }

  private[wallet] def encrypt(key: SecretKeySpec, value: Array[Byte]): Array[Byte] = {
    try {
      val cipher: Cipher = Cipher.getInstance(Algorithm)
      cipher.init(Cipher.ENCRYPT_MODE, key)
      cipher.doFinal(value)
    } catch {
      case NonFatal(e) =>
        throw new RuntimeException("encrypt error", e)
    }
  }

  private[wallet] def decrypt(key: SecretKeySpec, encryptedValue: Array[Byte]): Array[Byte] = {
    try {
      val cipher: Cipher = Cipher.getInstance(Algorithm)
      cipher.init(Cipher.DECRYPT_MODE, key)
      cipher.doFinal(encryptedValue)
    } catch {
      case NonFatal(e) =>
        throw new RuntimeException("decrypt error", e)
    }
  }
}

object WalletSecurity {
  val defaultKeySalt: String = "f1e1210c-5cfa-4b6e-bb8b-c141e5946b2f" //UUID.randomUUID().toString
  def apply(): WalletSecurity = new WalletSecurity(defaultKeySalt)
  def apply(keySalt: String): WalletSecurity = new WalletSecurity(keySalt)
}
