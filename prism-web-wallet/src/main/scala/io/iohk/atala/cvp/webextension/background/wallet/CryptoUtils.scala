package io.iohk.atala.cvp.webextension.background.wallet

import org.scalajs.dom.crypto
import org.scalajs.dom.crypto.{CryptoKey, KeyFormat}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.typedarray._

object CryptoUtils {
  private val PASSWORD_SALT = "kkmarcbr/a"
  private val AES_MODE = "AES-GCM"
  private val AES_KEY_LENGTH: Short = 256

  // GCM MODE
  private val GCM_IV_LENGTH = 12 // Initialization Vector
  private val TAG_LENGTH_BIT: Short = 128 // Authentication Tag
  // GCM MODE

  def initialAesGcm: crypto.AesGcmParams = {
    val ivBytes = Array.ofDim[Byte](GCM_IV_LENGTH)
    crypto.crypto.getRandomValues(ivBytes.toTypedArray)
    //Additional data is quite interesting it is data that can be authenticated data that won't be encrypted
    //but will be part of integrity, Hence I was thinking if we can.
    //We can use as origin so we know the request for encryption/decryption came from the same origin
    val additionalData = ""
    crypto.AesGcmParams(
      name = AES_MODE,
      iv = ivBytes.toTypedArray.buffer,
      additionalData = additionalData.getBytes.toTypedArray.buffer,
      tagLength = TAG_LENGTH_BIT
    )
  }

  def generateSecretKey(password: String)(implicit ec: ExecutionContext): Future[CryptoKey] = {
    val pbdkf2 =
      crypto.Pbkdf2Params("PBKDF2", PASSWORD_SALT.getBytes.toTypedArray.buffer, 100L, "SHA-512")

    for {
      pbkdf2Key <-
        crypto.crypto.subtle
          .importKey(
            KeyFormat.raw,
            password.getBytes.toTypedArray.buffer,
            "PBKDF2",
            false,
            js.Array(crypto.KeyUsage.deriveKey, crypto.KeyUsage.deriveBits)
          )
          .toFuture
          .asInstanceOf[Future[crypto.CryptoKey]]

      aesCtr = crypto.AesDerivedKeyParams(AES_MODE, AES_KEY_LENGTH)
      aesKey <-
        crypto.crypto.subtle
          .deriveKey(
            pbdkf2,
            pbkdf2Key,
            aesCtr,
            true,
            js.Array(crypto.KeyUsage.encrypt, crypto.KeyUsage.decrypt)
          )
          .toFuture
          .asInstanceOf[Future[crypto.CryptoKey]]
    } yield aesKey
  }

  def encrypt(key: CryptoKey, data: String)(implicit ec: ExecutionContext): Future[Array[Byte]] = {
    for {
      arrayBuffer <-
        crypto.crypto.subtle
          .encrypt(CryptoUtils.initialAesGcm, key, data.getBytes.toTypedArray.buffer)
          .toFuture
          .asInstanceOf[Future[ArrayBuffer]]
      arr = Array.ofDim[Byte](arrayBuffer.byteLength)
      _ = TypedArrayBuffer.wrap(arrayBuffer).get(arr)
    } yield arr
  }

  def decrypt(key: CryptoKey, bytes: Array[Byte])(implicit ec: ExecutionContext): Future[String] = {
    crypto.crypto.subtle
      .decrypt(CryptoUtils.initialAesGcm, key, bytes.toTypedArray.buffer)
      .toFuture
      .asInstanceOf[Future[ArrayBuffer]]
      .map { buffer =>
        val arr = Array.ofDim[Byte](buffer.byteLength)
        TypedArrayBuffer.wrap(buffer).get(arr)
        new String(arr, "UTF-8")
      }
  }
}
