package io.iohk.cef.crypto.low

import akka.util.ByteString

/**
  * Collection of helper methods to allow the encryption and decryption of some bytes
  *
  * NOTE:
  *   PARTIAL IMPLEMENTATION OF
  *   package object low
  */
trait CrypterPackageFragment extends CryptoAlgorithmPackageFragment {



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
  implicit class ByteStringCryptoOps(source: ByteString) {

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

}


