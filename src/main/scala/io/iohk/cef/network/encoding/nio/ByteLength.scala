package io.iohk.cef.network.encoding.nio

trait ByteLength[T] {
  def apply(t: T): Int
}

/**
  * These are the byte lengths written by ByteBuffers putInt, putChar, etc methods.
  * (as opposed to those in the JVM spec.)
  */
object ByteLength {
  implicit val lengthBoolean: ByteLength[Boolean] = _ => 1
  implicit val lengthByte: ByteLength[Byte] = _ => 1
  implicit val lengthShort: ByteLength[Short] = _ => 2
  implicit val lengthInt: ByteLength[Int] = _ => 4
  implicit val lengthLong: ByteLength[Long] = _ => 8
  implicit val lengthFloat: ByteLength[Float] = _ => 4
  implicit val lengthDouble: ByteLength[Double] = _ => 8
  implicit val lengthChar: ByteLength[Char] = _ => 2
  implicit val lengthString: ByteLength[String] = s => 4 + s.length * 2

  implicit def lengthArray[T](implicit lt: ByteLength[T]): ByteLength[Array[T]] =
    a => 4 + 4 + 4 + a.foldLeft(0)((sum, next) => sum + lt(next))
}
