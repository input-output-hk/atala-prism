package io.iohk.cef.cryptonew

import akka.util.ByteString

trait Decrypter extends DecryptAlgorithms {

  def decryptBytes(algorithm: DecryptAlgorithm)(source: ByteString, key: ByteString): ByteString =
    algorithm(source, key)

  implicit class ByteStringDecryptOps(source: ByteString) {
    def decryptWith(algorithm: DecryptAlgorithm, key: ByteString): ByteString =
      decryptBytes(algorithm)(source, key)
  }

}


