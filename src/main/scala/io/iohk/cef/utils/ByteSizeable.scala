package io.iohk.cef.utils
import io.iohk.cef.ledger.ByteStringSerializable

trait ByteSizeable[T] {
  def sizeInBytes(t: T): Int
}

object ByteSizeable {

  implicit def txByteSizeable[T](implicit byteStringSerializable: ByteStringSerializable[T]): ByteSizeable[T] =
    (t: T) => byteStringSerializable.encode(t).size
}
