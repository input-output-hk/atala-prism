package io.iohk.cef.utils
import io.iohk.cef.ledger.ByteStringSerializable

trait ByteSizeable[T] {
  def sizeInBytes(t: T): Int
}

object ByteSizeable {

  implicit def txByteSizeable[T](implicit byteStringSerializable: ByteStringSerializable[T]): ByteSizeable[T] =
    new ByteSizeable[T] {
      override def sizeInBytes(t: T): Int = byteStringSerializable.serialize(t).size
    }
}
