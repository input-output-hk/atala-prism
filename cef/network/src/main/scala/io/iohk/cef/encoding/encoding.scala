package io.iohk.cef

package object encoding {

  case class ValidEncoding[U](enc: U)

  trait Encoder[T, U] {
    def encode(t: T): U
  }

  trait Decoder[U, T] {
    def decode(u: U): T
  }

  def encode[T, U](t: T)(implicit enc: Encoder[T, U]): U = enc.encode(t)

  def decode[U, T](enc: U)(implicit dec: Decoder[U, T]): T = dec.decode(enc)

}
