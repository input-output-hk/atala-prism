package io.iohk.cef.crypto.low

import akka.util.ByteString

trait Signer extends SignAlgorithms {

  def signBytes(algorithm: SignAlgorithm)(source: ByteString, key: ByteString): ByteString =
    algorithm.sign(source, key)

  def validateSignatureOfBytes(algorithm: SignAlgorithm)(signature: ByteString, source: ByteString, key: ByteString): Boolean =
    algorithm.validate(signature, source, key)

  implicit class ByteStringSignOps(source: ByteString) {

    def signWith(algorithm: SignAlgorithm, key: ByteString): ByteString =
      signBytes(algorithm)(source, key)

  }

  implicit class ByteStringValidateSignatureOps(signature: ByteString) {

    /**
      * USAGE:
      *   signature.isSignatureOf(source).when(algorithm, key)
      */
    def isSignatureOf(source: ByteString): CanCheckIfIsSignatureOf =
      CanCheckIfIsSignatureOf(signature, source)

  }

  case class CanCheckIfIsSignatureOf(signature: ByteString, source: ByteString) {
    def when(algorithm: SignAlgorithm, key: ByteString): Boolean =
      validateSignatureOfBytes(algorithm)(signature, source, key)
  }

}


