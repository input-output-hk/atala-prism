package io.iohk.atala.prism.crypto

import java.security.SecureRandom

import scala.util.Try

import javax.crypto.{Cipher, SecretKeyFactory}
import javax.crypto.spec.{GCMParameterSpec, PBEKeySpec, SecretKeySpec}
import scala.util.Failure
import scala.util.Success

/**
  * AES256-GCM implementation.
  *
  * Can be used with:
  *   - string password (encryption key is derived using salt)
  *   - own key with provided IV
  */
object Aes {

  /**
    * Encrypted AES256-GCM data.
    *
    * @param encrypted Encrypted data
    * @param iv Initialization vector
    * @param salt Salt used to derive key from password string
    */
  case class AesEncryptedData(
      data: IndexedSeq[Byte],
      iv: IndexedSeq[Byte],
      salt: Option[IndexedSeq[Byte]]
  )

  case class AesException(
      message: String,
      cause: Option[Throwable] = None
  ) extends Exception(message, cause.orNull)

  // AES configuration
  val IV_SIZE = 64
  val KEY_SIZE = 256
  val AUTH_TAG_SIZE = 128

  // Key derivation configuration
  val SALT_SIZE = 64
  val ITERATION_COUNT = 10000

  private val random = new SecureRandom

  /**
    * Encrypt data with a key derived from a password string.
    */
  def encrypt(data: Array[Byte], password: String): Either[AesException, AesEncryptedData] = {
    val salt = randomBytes(SALT_SIZE)
    val iv = randomBytes(IV_SIZE)
    val key = deriveKey(password, salt)

    encrypt(data, key, iv).map(_.copy(salt = Some(salt.toIndexedSeq)))
  }

  /**
    * Encrypt data with key and IV.
    */
  def encrypt(data: Array[Byte], key: Array[Byte], iv: Array[Byte]): Either[AesException, AesEncryptedData] = {
    val cipher = createCipher(Cipher.ENCRYPT_MODE, key, iv)

    Try(cipher.doFinal(data).toIndexedSeq) match {
      case Failure(exception) => Left(AesException(exception.getMessage, Some(exception)))
      case Success(result) =>
        Right(
          AesEncryptedData(
            data = result,
            iv = iv.toIndexedSeq,
            salt = None
          )
        )
    }
  }

  /**
    * Decrypt data with a key derived from a password string.
    */
  def decrypt(encryptedData: AesEncryptedData, password: String): Either[AesException, Array[Byte]] = {
    for {
      salt <-
        encryptedData.salt.toRight(AesException("Couldn't derive a key from the password without salt.")).map(_.toArray)
      result <- decrypt(encryptedData, deriveKey(password, salt))
    } yield result
  }

  /**
    * Decrypt data with key and IV.
    */
  def decrypt(encryptedData: AesEncryptedData, key: Array[Byte]): Either[AesException, Array[Byte]] = {
    val iv = encryptedData.iv.toArray
    val cipher = createCipher(Cipher.DECRYPT_MODE, key, iv)

    Try(cipher.doFinal(encryptedData.data.toArray)) match {
      case Failure(exception) => Left(AesException(exception.getMessage, Some(exception)))
      case Success(result) => Right(result)
    }
  }

  /**
    * Derive a key from password string with given salt.
    */
  def deriveKey(password: String, salt: Array[Byte]): Array[Byte] = {
    val keySpec = new PBEKeySpec(password.toCharArray, salt, ITERATION_COUNT, KEY_SIZE)
    val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    secretKeyFactory.generateSecret(keySpec).getEncoded
  }

  /**
    * Cipher initialization.
    */
  private[crypto] def createCipher(
      mode: Int,
      key: Array[Byte],
      iv: Array[Byte]
  ): Cipher = {
    val derivedKey = new SecretKeySpec(key, "AES")
    val parameters = new GCMParameterSpec(AUTH_TAG_SIZE, iv)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")

    cipher.init(mode, derivedKey, parameters)
    cipher
  }

  /**
    * Generate a random byte array of the given length.
    */
  private[crypto] def randomBytes(length: Int): Array[Byte] = {
    val array = Array.ofDim[Byte](length)

    random.nextBytes(array)
    array
  }
}
