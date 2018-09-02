package io.iohk.cef.crypto.low

import akka.util.ByteString

/**
  * Contract all signing algorithm implementations should follow
  */
sealed trait SignAlgorithm {

  type PublicKey
  type PrivateKey

  /**
    * Computes the signature for the provided `source` bytes, using the provided `key`
    *
    * @param source     the bytes to be signed
    * @param key        the key to use for computing the signature
    *
    * @return a cryptographic signature for the provided `source` bytes
    */
  def sign(source: ByteString, key: PrivateKey): ByteString

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
  def isSignatureValid(signature: ByteString, source: ByteString, key: PublicKey): Boolean

}

/**
  * Companion object to SignAlgorithm, containing all the implemented `SignAlgorithm`
  */
object SignAlgorithm {

  /**
    * `SignAlgorithm` based on the composition of a `CryptoAlgorithm` and a `HashAlgorithm`
    *
    * @param cryptoAlgorithm  used to encrypt the signature, allowing the validation of the
    *                         signature author
    * @param hashAlgorithm    used to ensure the content of the message has not been meddled
    *                         with
    */
  case class Composed(cryptoAlgorithm: CryptoAlgorithm, hashAlgorithm: HashAlgorithm) extends SignAlgorithm {
    type PublicKey = cryptoAlgorithm.PrivateKey
    type PrivateKey = cryptoAlgorithm.PublicKey

    /** @inheritdoc */
    def sign(source: ByteString, key: PrivateKey): ByteString =
      cryptoAlgorithm.encrypt(hashAlgorithm.hash(source), key)

    /** @inheritdoc */
    def isSignatureValid(signature: ByteString, source: ByteString, key: PublicKey): Boolean =
      cryptoAlgorithm
        .decrypt(signature, key)
        .toOption
        .contains(hashAlgorithm.hash(source))

  }
}
