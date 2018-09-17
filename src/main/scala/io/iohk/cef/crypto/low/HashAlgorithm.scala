package io.iohk.cef.crypto.low

import java.security.MessageDigest

import akka.util.ByteString

/**
  * Contract all hashing algorithm implementations should follow
  */
sealed trait HashAlgorithm {

  /**
    * Hash the provided `source` bytes
    *
    * @param source the bytes to hash
    *
    * @return a hashed version of the `source` bytes
    */
  def hash(source: ByteString): ByteString

}

/**
  * Helper trait that allows the implementation of a `HashAlgorithm` basing it on
  * `Array[Byte]` instead of `ByteString`
  */
sealed trait ArrayBasedHashAlgorithm extends HashAlgorithm {

  /**
    * Hash the provided `source` bytes
    *
    * @param source the bytes to hash
    *
    * @return a hashed version of the `source` bytes
    */
  protected def hash(source: Array[Byte]): Array[Byte]

  /** @inheritdoc */
  override final def hash(source: ByteString): ByteString =
    ByteString(hash(source.toArray))
}

/**
  * Companion object to HashAlgorithm, containing all the implemented `HashAlgorithm`
  */
object HashAlgorithm {

  /**
    * Implementation of the `SHA-256` `HashAlgorithm`
    */
  case object SHA256 extends ArrayBasedHashAlgorithm {

    /** @inheritdoc */
    override final protected def hash(source: Array[Byte]): Array[Byte] = {
      // TODO: This is unsafe on huge inputs
      MessageDigest
        .getInstance("SHA-256")
        .digest(source)
    }
  }
}
