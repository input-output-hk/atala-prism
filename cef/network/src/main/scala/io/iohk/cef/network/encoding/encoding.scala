package io.iohk.cef.network

package object encoding {

  case class Codec[T, U](encoder: Encoder[T, U], decoder: Decoder[U, T])

  trait Encoder[T, U] {
    self =>

    def encode(t: T): U

    def andThen[S](that: Encoder[U, S]): Encoder[T, S] =
      (t: T) => that.encode(self.encode(t))
  }

  trait Decoder[U, T] {
    self =>

    def decode(u: U): T

    def andThen[S](that: Decoder[T, S]): Decoder[U, S] =
      (u: U) => that.decode(self.decode(u))
  }

  trait StreamDecoder[U, T] extends Decoder[U, T] {
    def decodeStream(u: U): Seq[T]
  }

  def encode[T, U](t: T)(implicit enc: Encoder[T, U]): U =
    try {
      enc.encode(t)
    } catch {
      case t: Throwable =>
        throw new EncodingException(t)
    }

    def decode[U, T](enc: U)(implicit dec: Decoder[U, T]): T = try {
      dec.decode(enc)
    } catch {
      case t: Throwable =>
        throw new DecodingException(t)
    }

    class EncodingException(cause: Throwable) extends RuntimeException(cause)

    class DecodingException(cause: Throwable) extends RuntimeException(cause)

    type ByteEncoder[T] = Encoder[T, Array[Byte]]
}
