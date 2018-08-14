package io.iohk.cef.crypto
import akka.util.ByteString

/**
  * Collection of low-level cryptographic primitives
  */
package object low {



      /*           *
          HASHING
       *           */


  /**
    * Hash the provided `source` bytes with the provided `algorithm`
    *
    * @param algorithm  the hashing algorithm to be used to perform the hashing
    * @param source     the bytes to be encrypted
    *
    * @return a hashed version of the `source` bytes
    */
  def hashBytes(algorithm: HashAlgorithm)(source: ByteString): ByteString =
    algorithm(source)




  /**
    * Extends `ByteString` with hashing related methods
    *
    * @param source     the bytes to be hashed
    */
  implicit class ByteStringHashOps(val source: ByteString) extends AnyVal {

  /**
    * Computes the hash with the provided `algorithm`
    *
    * @param algorithm  the hashing algorithm to be used to perform the hashing
    *
    * @return a hashed version of `source`
    */
    def hashWith(algorithm: HashAlgorithm): ByteString =
      hashBytes(algorithm)(source)

  }



      /*           *
          ENCRYPT
          DECRYPT
       *           */


  /**
    * Encrypts the provided `source` bytes with the provided `algorithm` and `key`
    *
    * @param algorithm  the encrypting algorithm to be used to perform the encryption
    * @param source     the bytes to be encrypted
    * @param key        the cryptographic key to be used to perform the encryption
    *
    * @return an encrypted version of the `source` bytes
    */
  def encryptBytes(algorithm: CryptoAlgorithm)(source: ByteString, key: ByteString): ByteString =
    algorithm.encrypt(source, key)

  /**
    * Decrypts the provided `source` bytes with the provided `algorithm` and `key`
    *
    * @param algorithm  the decrypting algorithm to be used to perform the decryption
    * @param source     the encrypted string of bytes to be decrypted
    * @param key        the cryptographic key to be used to perform the decryption
    *
    * @return a decrypted version of the encrypted `source` if the `algorithm` and `key` are
    *         correct, or gibberish otherwise
    */
  def decryptBytes(algorithm: CryptoAlgorithm)(source: ByteString, key: ByteString): ByteString =
    algorithm.decrypt(source, key)




  /**
    * Extends `ByteString` with encryption and decryption related methods
    *
    * @param source     the bytes to be encrypted or decrypted
    */
  implicit class ByteStringCryptoOps(val source: ByteString) extends AnyVal {

    /**
      * Encrypts `source` with the provided `algorithm` and `key`
      *
      * @param algorithm  the encrypting algorithm to be used to perform the encryption
      * @param key        the cryptographic key to be used to perform the encryption
      *
      * @return an encrypted version of the `source` bytes
      */
    def encryptWith(algorithm: CryptoAlgorithm, key: ByteString): ByteString =
      encryptBytes(algorithm)(source, key)


    /**
      * Decrypts the encrypted `source` with the provided `algorithm` and `key`
      *
      * @param algorithm  the decrypting algorithm to be used to perform the decryption
      * @param key        the cryptographic key to be used to perform the decryption
      *
      * @return a decrypted version of the encrypted `source` if the `algorithm` and `key` are
      *         correct, or gibberish otherwise
      */
    def decryptWith(algorithm: CryptoAlgorithm, key: ByteString): ByteString =
      decryptBytes(algorithm)(source, key)

  }



      /*           *
          SIGNING
       *           */


  /**
    * Computes the signature for the provided `source` bytes, using the provided
    * `algorithm` and `key`
    *
    * @param algorithm  the signing algorithm to be used to generate the signature
    * @param source     the bytes to be signed
    * @param key        the cryptographic key to be used to perform the signature
    *
    * @return a cryptographic signature for the provided `source` bytes
    */
  def signBytes(algorithm: SignAlgorithm)(source: ByteString, key: ByteString): ByteString =
    algorithm.sign(source, key)

  /**
    * Validates the `signature` of the provided `source` bytes, using the `algorithm` and `key`
    *
    * @param algorithm  the signing algorithm to be used to validate the signature
    * @param signature  the signature that need to be checked against `source`
    * @param source     the bytes that need to be validated against the `signature`
    * @param key        the cryptographic key to be used to perform the validation
    *
    * @return `true` if the `signature` corresponds to the `source` when validated using
    *         `algorithm` and `key`. `false` otherwise
    */
  def validateSignatureOfBytes(algorithm: SignAlgorithm)(signature: ByteString, source: ByteString, key: ByteString): Boolean =
    algorithm.validate(signature, source, key)




  /**
    * Extends `ByteString` with siging related methods
    *
    * @param source     the bytes to be signed
    */
  implicit class ByteStringSignOps(val source: ByteString) extends AnyVal {

    /**
      * Computes the signature of `source`, using the provided `algorithm` and `key`
      *
      * @param algorithm  the signing algorithm to be used to generate the signature
      * @param key        the cryptographic key to be used to perform the signature
      *
      * @return a cryptographic signature for the provided `source` bytes
      */
    def signWith(algorithm: SignAlgorithm, key: ByteString): ByteString =
      signBytes(algorithm)(source, key)

  }

  /**
    * Extends `ByteString` with signature validation related methods
    *
    * @param signature  the signature that need to be checked
    */
  implicit class ByteStringValidateSignatureOps(val signature: ByteString) extends AnyVal {

    /**
      * Starting point for a small DSL used to compute the validity of a
      * signature.
      *
      * If it was a normal `def` and not a DSL, the params and return would be:
      *
      * @param signature  the signature that need to be checked against `source`
      * @param source     the bytes that need to be validated against the `signature`
      * @param algorithm  the signing algorithm to be used to validate the signature
      * @param key        the cryptographic key to be used to perform the validation
      *
      * @return `true` if the `signature` corresponds to the `source` when validated using
      *         `algorithm` and `key`. `false` otherwise
      *
      * USAGE:
      *   signature.isSignatureOf(source).when(algorithm, key)
      */
    def isSignatureOf(source: ByteString): CanCheckIfIsSignatureOf =
      CanCheckIfIsSignatureOf(signature, source)

  }

  /** Helper case class for the implementation of the signature validation DSL */
  case class CanCheckIfIsSignatureOf(signature: ByteString, source: ByteString) {
    def when(algorithm: SignAlgorithm, key: ByteString): Boolean =
      validateSignatureOfBytes(algorithm)(signature, source, key)
  }


}
