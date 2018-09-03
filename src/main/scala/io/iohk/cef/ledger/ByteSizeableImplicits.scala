package io.iohk.cef.ledger
import io.iohk.cef.utils.ByteSizeable

object ByteSizeableImplicits {

  implicit def byteSizeable[T](implicit serializer: ByteStringSerializable[T]): ByteSizeable[T] =
    new ByteSizeable[T] {
      override def sizeInBytes(t: T): Int = serializer.serialize(t).size
    }
}
