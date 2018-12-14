package io.iohk.cef.codecs.nio.components

private[components] object ByteLength {
  val lengthBoolean: Int = 1
  val lengthByte: Int = 1
  val lengthShort: Int = 2
  val lengthInt: Int = 4
  val lengthLong: Int = 8
  val lengthFloat: Int = 4
  val lengthDouble: Int = 8
  val lengthChar: Int = 2
  val lengthString: String => Int = s => 4 + s.length * 2
}
