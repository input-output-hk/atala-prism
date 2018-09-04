package io.iohk.cef.crypto

import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PrivateKey, PublicKey}

import akka.util.ByteString

/**
  * Collection of low-level cryptographic primitives
  */
package object low {

  /**
    * Represents data that has been encrypted.
    */
  class EncryptedData(val value: ByteString) extends AnyVal

  /**
    * Represents a cryptographic signature.
    */
  class DigitalSignature(val value: ByteString) extends AnyVal

  // TODO: Support different key types
  def decodePublicKey(bytes: Array[Byte]): PublicKey = {
    val publicKeySpec = new X509EncodedKeySpec(bytes)
    val keyFactory = KeyFactory.getInstance("RSA")
    keyFactory.generatePublic(publicKeySpec)
  }

  /** HASHING **/
  /**
    * Hash the provided `source` bytes with the provided `algorithm`
    *
    * @param algorithm  the hashing algorithm to be used to perform the hashing
    * @param source     the bytes to HASH
    *
    * @return a hashed version of the `source` bytes
    */
  def hashBytes(algorithm: HashAlgorithm)(source: ByteString): ByteString =
    algorithm.hash(source)

  /** ENCRYPTION **/
  /**
    * Encrypts the provided `source` bytes with the provided `algorithm` and `param`
    *
    * @param algorithm  the encrypting algorithm to be used to perform the encryption
    * @param source     the bytes to be encrypted
    * @param key        the key to encrypt the data
    *
    * @return an encrypted version of the `source` bytes
    */
  def encryptBytes(algorithm: CryptoAlgorithm)(source: ByteString, key: PublicKey): EncryptedData =
    algorithm.encrypt(source, key)

  /**
    * Decrypts the provided `source` bytes with the provided `algorithm` and `param`
    *
    * @param algorithm  the decrypting algorithm to be used to perform the decryption
    * @param source     the encrypted string of bytes to be decrypted
    * @param key        the key to decrypt the source
    *
    * @return a decrypted version of the encrypted `source` if the `algorithm` and `key` are able
    *          to decrypt the source
    */
  def decryptBytes(
      algorithm: CryptoAlgorithm)(source: EncryptedData, key: PrivateKey): Either[algorithm.DecryptError, ByteString] =
    algorithm.decrypt(source, key)

  /** SIGNING **/
  /**
    * Computes the signature for the provided `source` bytes, using the provided
    * `algorithm` and `param`
    *
    * @param algorithm  the signing algorithm to be used to generate the signature
    * @param source     the bytes to be signed
    * @param key        the key used to create the signature
    *
    * @return a cryptographic signature for the provided `source` bytes
    */
  def signBytes(algorithm: SignAlgorithm)(source: ByteString, key: PrivateKey): DigitalSignature =
    algorithm.sign(source, key)

  /**
    * Validates the `signature` of the provided `source` bytes, using the `algorithm` and `key`
    *
    * @param algorithm  the signing algorithm to be used to validate the signature
    * @param signature  the signature that need to be checked against `source`
    * @param source     the bytes that need to be validated against the `signature`
    * @param key        the key used to validate the signature
    *
    * @return `true` if the `signature` corresponds to the `source` when validated using
    *         `algorithm` and `key`. `false` otherwise
    */
  def isBytesSignatureValid(
      algorithm: SignAlgorithm)(signature: DigitalSignature, source: ByteString, key: PublicKey): Boolean =
    algorithm.isSignatureValid(signature, source, key)

}
