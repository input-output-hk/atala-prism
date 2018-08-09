package io.iohk.cef.crypto.low

import akka.util.ByteString

trait CryptoAlgorithms {

  sealed trait CryptoAlgorithm {

    def encrypt(input: ByteString, key: ByteString): ByteString

    def decrypt(input: ByteString, key: ByteString): ByteString

  }

  sealed trait ArrayBasedCryptoAlgorithm extends CryptoAlgorithm {

    protected def encrypt(input: Array[Byte], key: Array[Byte]): Array[Byte]

    override final def encrypt(input: ByteString, key: ByteString): ByteString =
      ByteString(encrypt(input.toArray, key.toArray))

    protected def decrypt(input: Array[Byte], key: Array[Byte]): Array[Byte]

    override final def decrypt(input: ByteString, key: ByteString): ByteString =
      ByteString(decrypt(input.toArray, key.toArray))

  }

  object CryptoAlgorithm {


  }

}

