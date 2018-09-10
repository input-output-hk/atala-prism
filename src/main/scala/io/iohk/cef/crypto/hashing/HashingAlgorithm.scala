package io.iohk.cef.crypto
package hashing

import akka.util.ByteString

private[crypto] trait HashAlgorithm {

  def hash(source: ByteString): HashBytes

}

private[crypto] trait ArrayBasedHashAlgorithm extends HashAlgorithm {

  protected def hash(source: Array[Byte]): Array[Byte]

  override final def hash(source: ByteString): HashBytes =
    HashBytes(ByteString(hash(source.toArray)))
}

private[crypto] case class HashBytes(bytes: ByteString)
