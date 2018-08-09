package io.iohk.cef.cryptonew

import akka.util.ByteString

trait DecryptAlgorithms {

  sealed trait DecryptAlgorithm {

    def apply(input: ByteString, key: ByteString): ByteString

  }

  sealed trait ArrayBasedDecryptAlgorithm extends DecryptAlgorithm {
    protected def apply(input: Array[Byte], key: Array[Byte]): Array[Byte]

    override final def apply(input: ByteString, key: ByteString): ByteString =
      ByteString(apply(input.toArray, key.toArray))
  }

  object DecryptAlgorithm {


  }

}

