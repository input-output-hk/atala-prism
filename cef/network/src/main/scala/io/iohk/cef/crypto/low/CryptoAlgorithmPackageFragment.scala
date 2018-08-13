package io.iohk.cef.crypto.low

import akka.util.ByteString



/**
  * Collection of encrypting/decrypting algorithm pairs
  *
  * NOTE:
  *   PARTIAL IMPLEMENTATION OF
  *   package object low
  */
trait CryptoAlgorithmPackageFragment {

  /**
    * Contract all encrypting/decrypting algorithm pair implementations should follow
    */
  sealed trait CryptoAlgorithm {

    /**
      * Encrypts the provided `source` bytes with the provided `key`
      *
      * @param source     the bytes to be encrypted
      * @param key        the cryptographic key to be used to perform the encryption
      *
      * @return an encrypted version of the `source` bytes
      */
    def encrypt(source: ByteString, key: ByteString): ByteString

    /**
      * Decrypts the provided `source` bytes with the provided `key`
      *
      * @param source     the encrypted string of bytes to be decrypted
      * @param key        the cryptographic key to be used to perform the decryption
      *
      * @return a decrypted version of the encrypted `source` if the `algorithm` and `key` are
      *         correct, or gibberish otherwise
      */
    def decrypt(source: ByteString, key: ByteString): ByteString

  }


  /**
    * Helper trait that allows the implementation of a `CryptoAlgorithm` basing it on
    * `Array[Byte]` instead of `ByteString`
    */
  sealed trait ArrayBasedCryptoAlgorithm extends CryptoAlgorithm {

    /**
      * Encrypts the provided `source` bytes with the provided `key`
      *
      * @param source     the bytes to be encrypted
      * @param key        the cryptographic key to be used to perform the encryption
      *
      * @return an encrypted version of the `source` bytes
      */
    protected def encrypt(source: Array[Byte], key: Array[Byte]): Array[Byte]

    /** @inheritdoc */
    override final def encrypt(source: ByteString, key: ByteString): ByteString =
      ByteString(encrypt(source.toArray, key.toArray))

    /**
      * Decrypts the provided `source` bytes with the provided `key`
      *
      * @param source     the encrypted string of bytes to be decrypted
      * @param key        the cryptographic key to be used to perform the decryption
      *
      * @return a decrypted version of the encrypted `source` if the `algorithm` and `key` are
      *         correct, or gibberish otherwise
      */
    protected def decrypt(source: Array[Byte], key: Array[Byte]): Array[Byte]

    /** @inheritdoc */
    override final def decrypt(source: ByteString, key: ByteString): ByteString =
      ByteString(decrypt(source.toArray, key.toArray))

  }


  /**
    * Companion object to CryptoAlgorithm, containing all the implemented `CryptoAlgorithm`
    */
  object CryptoAlgorithm {


  }

}

