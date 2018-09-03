package io.iohk.cef.crypto.low

import java.security.{PrivateKey, PublicKey, SecureRandom, Signature}

import akka.util.ByteString

/**
  * Contract all signing algorithm implementations should follow
  */
sealed trait SignAlgorithm {

  /**
    * Computes the signature for the provided `source` bytes, using the provided `key`
    *
    * @param source     the bytes to be signed
    * @param key        the key to use for computing the signature
    *
    * @return a cryptographic signature for the provided `source` bytes
    */
  def sign(source: ByteString, key: PrivateKey): DigitalSignature

  /**
    * Validates the `signature` of the provided `source` bytes, using the provided `key`
    *
    * @param signature  the signature that need to be checked against `source`
    * @param source     the bytes that need to be validated against the `signature`
    * @param key        the key used to verify the signature
    *
    * @return `true` if the `signature` corresponds to the `source` when validated using
    *         the provided `key`, `false` otherwise
    */
  def isSignatureValid(signature: DigitalSignature, source: ByteString, key: PublicKey): Boolean

}

/**
  * Companion object to SignAlgorithm, containing all the implemented `SignAlgorithm`
  */
object SignAlgorithm {

  private val Algorithm = "SHA256withRSA"

  class RSA(secureRandom: SecureRandom) extends SignAlgorithm {
    override def sign(source: ByteString, key: PrivateKey): DigitalSignature = {
      val signer = Signature.getInstance(Algorithm)
      signer.initSign(key)

      // TODO: Find a way to use buffers in order to not crash on huge inputs
      signer.update(source.toArray)

      val result = signer.sign()
      new DigitalSignature(ByteString(result))
    }

    override def isSignatureValid(signature: DigitalSignature, source: ByteString, key: PublicKey): Boolean = {
      val signer = Signature.getInstance(Algorithm)
      signer.initVerify(key)

      // TODO: Find a way to use buffers in order to not crash on huge inputs
      signer.update(source.toArray)

      try {
        signer.verify(signature.value.toArray)
      } catch {
        case _: Throwable => false
      }
    }
  }
}
