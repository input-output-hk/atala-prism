package io.iohk.cef.cryptonew

import akka.util.ByteString

trait EncryptAlgorithms {

  sealed trait EncryptAlgorithm {

    def apply(input: ByteString, key: ByteString): ByteString

  }

  sealed trait ArrayBasedEncryptAlgorithm extends EncryptAlgorithm {
    protected def apply(input: Array[Byte], key: Array[Byte]): Array[Byte]

    override final def apply(input: ByteString, key: ByteString): ByteString =
      ByteString(apply(input.toArray, key.toArray))
  }

  object EncryptAlgorithm {


  }

}

