package io.iohk.cef.cryptonew

import akka.util.ByteString

trait Crypter extends CryptoAlgorithms {

  def encryptBytes(algorithm: CryptoAlgorithm)(source: ByteString, key: ByteString): ByteString =
    algorithm.encrypt(source, key)

  def decryptBytes(algorithm: CryptoAlgorithm)(source: ByteString, key: ByteString): ByteString =
    algorithm.decrypt(source, key)

  implicit class ByteStringCryptoOps(source: ByteString) {

    def encryptWith(algorithm: CryptoAlgorithm, key: ByteString): ByteString =
      encryptBytes(algorithm)(source, key)

    def decryptWith(algorithm: CryptoAlgorithm, key: ByteString): ByteString =
      decryptBytes(algorithm)(source, key)

  }

}


