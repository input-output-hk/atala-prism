package io.iohk.cef.utils
import io.iohk.cef.codecs.nio._

trait ByteSizeable[T] {
  def sizeInBytes(t: T): Int
}

object ByteSizeable {

  implicit def txByteSizeable[T](implicit byteStringSerializable: NioEncDec[T]): ByteSizeable[T] =
    (t: T) => byteStringSerializable.encode(t).toArray.length

}
