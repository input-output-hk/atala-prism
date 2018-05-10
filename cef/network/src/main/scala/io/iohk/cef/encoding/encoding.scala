package io.iohk.cef

package object encoding {

  trait Encoder[T, U] {
    self =>

    def encode(t: T): U

    def andThen[S](that: Encoder[U, S]): Encoder[T, S] =
      new Encoder[T, S] {
        override def encode(t: T): S = that.encode(self.encode(t))
      }
  }

  trait Decoder[U, T] {
    self =>

    def decode(u: U): T

    def andThen[S](that: Decoder[T, S]): Decoder[U, S] =
      new Decoder[U, S] {
        override def decode(u: U): S = that.decode(self.decode(u))
      }
  }

  // TODO would it be preferable to use Try, Either or Throwables here?

  def encode[T, U](t: T)(implicit enc: Encoder[T, U]): U = enc.encode(t)

  def decode[U, T](enc: U)(implicit dec: Decoder[U, T]): T = dec.decode(enc)

  type ByteEncoder[T] = Encoder[T, Array[Byte]]
}
