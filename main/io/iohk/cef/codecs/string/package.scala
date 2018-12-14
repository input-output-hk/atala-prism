package io.iohk.cef.codecs

package object string {
  type Show[T] = Encoder[T, String]
  type Parse[T] = Decoder[String, T]
  trait Format[T] extends Show[T] with Parse[T]
}
