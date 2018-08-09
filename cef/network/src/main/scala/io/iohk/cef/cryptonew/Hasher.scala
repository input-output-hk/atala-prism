package io.iohk.cef.cryptonew

import akka.util.ByteString

trait Hasher extends HashAlgorithms {

  def hashBytes(algorithm: HashAlgorithm)(source: ByteString): ByteString =
    algorithm(source)

  implicit class ByteStringOps(source: ByteString) {
    def hashWith(algorithm: HashAlgorithm): ByteString =
      hashBytes(algorithm)(source)
  }

}

