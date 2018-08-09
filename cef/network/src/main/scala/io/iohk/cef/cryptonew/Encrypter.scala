package io.iohk.cef.cryptonew

import akka.util.ByteString

trait Encrypter extends EncryptAlgorithms {

  def encryptBytes(algorithm: EncryptAlgorithm)(source: ByteString, key: ByteString): ByteString =
    algorithm(source, key)

  implicit class ByteStringEncryptOps(source: ByteString) {
    def encryptWith(algorithm: EncryptAlgorithm, key: ByteString): ByteString =
      encryptBytes(algorithm)(source, key)
  }

}


