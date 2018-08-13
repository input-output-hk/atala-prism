package io.iohk.cef.crypto.low

import akka.util.ByteString


/**
  * Collection of helper methods to allow the hashing of byte strings
  *
  * NOTE:
  *   PARTIAL IMPLEMENTATION OF
  *   package object low
  */
trait HasherPackageFragment extends HashAlgorithmPackageFragment {

  /**
    * Hash the provided `source` bytes with the provided `algorithm`
    *
    * @param algorithm  the hashing algorithm to be used to perform the hashing
    * @param source     the bytes to be encrypted
    *
    * @return a hashed version of the `source` bytes
    */
  def hashBytes(algorithm: HashAlgorithm)(source: ByteString): ByteString =
    algorithm(source)




  /**
    * Extends `ByteString` with hashing related methods
    *
    * @param source     the bytes to be hashed
    */
  implicit class ByteStringHashOps(source: ByteString) {

  /**
    * Computes the hash with the provided `algorithm`
    *
    * @param algorithm  the hashing algorithm to be used to perform the hashing
    *
    * @return a hashed version of `source`
    */
    def hashWith(algorithm: HashAlgorithm): ByteString =
      hashBytes(algorithm)(source)

  }

}

