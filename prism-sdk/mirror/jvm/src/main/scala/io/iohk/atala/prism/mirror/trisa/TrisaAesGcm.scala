package io.iohk.atala.prism.mirror.trisa

import java.security.SecureRandom

import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.KeyParameter

import io.iohk.atala.prism.crypto.Aes

/**
  * AES256-GCM implementation for TRISA.
  * Code was ported from https://github.com/trisacrypto/trisa/blob/master/pkg/trisa/crypto/aesgcm/aesgcm.go
  * and encrypted data was tested with https://github.com/trisacrypto/trisa/blob/master/pkg/trisa/crypto/aesgcm/aesgcm_test.go
  * to prove compatibility.
  *
  * Despite this, there are some inconsistencies in original source code:
  *   - "cipher secret (encrypted secret using pub key of receiver)" - but the key is created
  *     as a random byte array at line 22.
  *   - nonce is a random bytes array, so it's more like IV
  *   - HMAC key is duplicated with the main key
  */
object TrisaAesGcm {

  /**
    * Encrypted TRISA data.
    *
    * @param encrypted encrypted data + IV
    * @param cipherSecret key
    * @param hmac HMAC signature
    * @param hmacSecret key
    */
  case class TrisaAesGcmEncryptedData(
      data: IndexedSeq[Byte],
      cipherSecret: IndexedSeq[Byte],
      hmac: IndexedSeq[Byte],
      hmacSecret: IndexedSeq[Byte]
  )

  case class TrisaAesGcmException(
      message: String,
      cause: Option[Throwable] = None
  ) extends Exception(message, cause.orNull)

  // AES configuration
  val KEY_SIZE = 32
  val IV_SIZE = 12

  private val random = new SecureRandom

  /**
    * Encrypt data and create an HMAC signature with a randomly generated key.
    */
  def encrypt(plainText: Array[Byte]): Either[TrisaAesGcmException, TrisaAesGcmEncryptedData] = {
    val key = randomBytes(KEY_SIZE)
    val iv = randomBytes(IV_SIZE)

    for {
      aes <- Aes.encrypt(plainText, key, iv).left.map(error => TrisaAesGcmException(error.getMessage, error.cause))
      hmac = hmacSha256(aes.data.toArray, key)
    } yield TrisaAesGcmEncryptedData(
      data = aes.data ++ iv,
      cipherSecret = key.toIndexedSeq,
      hmac = hmac.toIndexedSeq,
      hmacSecret = key.toIndexedSeq
    )
  }

  /**
    * Decrypt data and validate an HMAC signature.
    */
  def decrypt(encryptedData: TrisaAesGcmEncryptedData): Either[TrisaAesGcmException, Array[Byte]] = {
    val size = encryptedData.data.size
    val data = encryptedData.data.slice(0, size - IV_SIZE)
    val iv = encryptedData.data.slice(size - IV_SIZE, size)
    val hmac = hmacSha256(data.toArray, encryptedData.hmacSecret.toArray)

    val aes = Aes.AesEncryptedData(
      data = data,
      iv = iv,
      salt = None
    )

    for {
      _ <- Either.cond(encryptedData.hmac == hmac.toIndexedSeq, (), TrisaAesGcmException("HMAC signature mismatch."))
      result <-
        Aes
          .decrypt(aes, encryptedData.cipherSecret.toArray)
          .left
          .map(error => TrisaAesGcmException(error.getMessage, error.cause))
    } yield result
  }

  /**
    * Create HMAC SHA256 signature.
    */
  private[trisa] def hmacSha256(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val hmac = new HMac(new SHA256Digest)
    hmac.init(new KeyParameter(key.toArray))
    hmac.update(data.toArray, 0, data.size)
    val out = new Array[Byte](32)
    hmac.doFinal(out, 0)
    out
  }

  /**
    * Generate a random byte array of the given length.
    */
  private[trisa] def randomBytes(size: Int): Array[Byte] = {
    val array = Array.ofDim[Byte](size)
    random.nextBytes(array)
    array
  }

}
