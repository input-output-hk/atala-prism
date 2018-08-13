package io.iohk.cef.crypto.low

import akka.util.ByteString

/**
  * Collection of helper methods to allow the signing of some bytes and the validation of signatures
  *
  * NOTE:
  *   PARTIAL IMPLEMENTATION OF
  *   package object low
  */
trait SignerPackageFragment extends SignAlgorithmPackageFragment {


  /**
    * Computes the signature for the provided `source` bytes, using the provided
    * `algorithm` and `key`
    *
    * @param algorithm  the signing algorithm to be used to generate the signature
    * @param source     the bytes to be signed
    * @param key        the cryptographic key to be used to perform the signature
    *
    * @return a cryptographic signature for the provided `source` bytes
    */
  def signBytes(algorithm: SignAlgorithm)(source: ByteString, key: ByteString): ByteString =
    algorithm.sign(source, key)

  /**
    * Validates the `signature` of the provided `source` bytes, using the `algorithm` and `key`
    *
    * @param algorithm  the signing algorithm to be used to validate the signature
    * @param signature  the signature that need to be checked against `source`
    * @param source     the bytes that need to be validated against the `signature`
    * @param key        the cryptographic key to be used to perform the validation
    *
    * @return `true` if the `signature` corresponds to the `source` when validated using
    *         `algorithm` and `key`. `false` otherwise
    */
  def validateSignatureOfBytes(algorithm: SignAlgorithm)(signature: ByteString, source: ByteString, key: ByteString): Boolean =
    algorithm.validate(signature, source, key)




  /**
    * Extends `ByteString` with siging related methods
    *
    * @param source     the bytes to be signed
    */
  implicit class ByteStringSignOps(source: ByteString) {

    /**
      * Computes the signature of `source`, using the provided `algorithm` and `key`
      *
      * @param algorithm  the signing algorithm to be used to generate the signature
      * @param key        the cryptographic key to be used to perform the signature
      *
      * @return a cryptographic signature for the provided `source` bytes
      */
    def signWith(algorithm: SignAlgorithm, key: ByteString): ByteString =
      signBytes(algorithm)(source, key)

  }

  /**
    * Extends `ByteString` with signature validation related methods
    *
    * @param signature  the signature that need to be checked
    */
  implicit class ByteStringValidateSignatureOps(signature: ByteString) {

    /** Starting point for a small DSL used to compute the validity of a
      * signature.
      *
      * If it was a normal `def` and not a DSL, the params and return would be:
      *
      * @param signature  the signature that need to be checked against `source`
      * @param source     the bytes that need to be validated against the `signature`
      * @param algorithm  the signing algorithm to be used to validate the signature
      * @param key        the cryptographic key to be used to perform the validation
      *
      * @return `true` if the `signature` corresponds to the `source` when validated using
      *         `algorithm` and `key`. `false` otherwise
      *
      * USAGE:
      *   signature.isSignatureOf(source).when(algorithm, key)
      */
    def isSignatureOf(source: ByteString): CanCheckIfIsSignatureOf =
      CanCheckIfIsSignatureOf(signature, source)

  }

  /** Helper case class for the implementation of the signature validation DSL */
  case class CanCheckIfIsSignatureOf(signature: ByteString, source: ByteString) {
    def when(algorithm: SignAlgorithm, key: ByteString): Boolean =
      validateSignatureOfBytes(algorithm)(signature, source, key)
  }

}


