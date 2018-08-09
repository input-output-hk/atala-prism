package io.iohk.cef.cryptonew

import akka.util.ByteString

trait SignAlgorithms {

  sealed trait SignAlgorithm {

    def sign(input: ByteString, key: ByteString): ByteString

    def validate(signature: ByteString, input: ByteString, key: ByteString): Boolean

  }

  sealed trait ArrayBasedSignAlgorithm extends SignAlgorithm {

    protected def sign(input: Array[Byte], key: Array[Byte]): Array[Byte]

    override final def sign(input: ByteString, key: ByteString): ByteString =
      ByteString(sign(input.toArray, key.toArray))

    protected def validate(signature: Array[Byte], input: Array[Byte], key: Array[Byte]): Boolean

    override final def validate(signature: ByteString, input: ByteString, key: ByteString): Boolean =
      validate(signature.toArray, input.toArray, key.toArray)

  }

  object SignAlgorithm {

    case class Composed(cryptoAlgorithm: CryptoAlgorithm, hashAlgorithm: HashAlgorithm) extends SignAlgorithm {

      def sign(input: ByteString, key: ByteString): ByteString =
        cryptoAlgorithm.encrypt(
          hashAlgorithm(input),
          key)

      def validate(signature: ByteString, input: ByteString, key: ByteString): Boolean =
        cryptoAlgorithm.decrypt(signature, key) == hashAlgorithm(input)
    }

  }

}

