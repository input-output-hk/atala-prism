package io.iohk.cef.crypto.low

import akka.util.ByteString

trait Hasher extends HashAlgorithms {

  def hashBytes(algorithm: HashAlgorithm)(source: ByteString): ByteString =
    algorithm(source)

  implicit class ByteStringHashOps(source: ByteString) {
    def hashWith(algorithm: HashAlgorithm): ByteString =
      hashBytes(algorithm)(source)
  }

}

