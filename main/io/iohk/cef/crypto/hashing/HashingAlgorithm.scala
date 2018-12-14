package io.iohk.cef.crypto
package hashing

import akka.util.ByteString

/** Defines a contract on how a hashing algorithm should be implemented */
private[crypto] trait HashAlgorithm {

  /** returns a hash of `source`, wrapped in a [[HashBytes]] entity */
  def hash(source: ByteString): HashBytes

}

private[crypto] trait ArrayBasedHashAlgorithm extends HashAlgorithm {

  protected def hash(source: Array[Byte]): Array[Byte]

  override final def hash(source: ByteString): HashBytes =
    HashBytes(ByteString(hash(source.toArray)))
}

private[crypto] case class HashBytes(bytes: ByteString)
