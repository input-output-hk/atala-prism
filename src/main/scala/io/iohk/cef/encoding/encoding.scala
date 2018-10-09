package io.iohk.cef

package object encoding {

  class Codec[T, U](val encoder: Encoder[T, U], val decoder: Decoder[U, T])

  class StreamCodec[T, U](val encoder: Encoder[T, U], val decoder: StreamDecoder[U, T])

  trait EncoderDecoder[T, U] extends Encoder[T, U] with Decoder[U, T] {
    def andThen[S](thatEncoder: Encoder[U, S], thatDecoder: Decoder[S, U]): EncoderDecoder[T, S] = {
      val newDecoder = thatDecoder andThen this
      val newEncoder = this andThen thatEncoder
      new EncoderDecoder[T, S] {
        override def encode(t: T): S = newEncoder.encode(t)
        override def decode(s: S): Option[T] = newDecoder.decode(s)
      }
    }
  }

  trait Encoder[T, U] {
    self =>

    def encode(t: T): U

    def andThen[S](that: Encoder[U, S]): Encoder[T, S] =
      (t: T) => that.encode(self.encode(t))
  }

  trait Decoder[U, T] {
    self =>

    def decode(u: U): Option[T]

    def andThen[S](that: Decoder[T, S]): Decoder[U, S] =
      (u: U) => self.decode(u).flatMap(that.decode)
  }

  trait StreamDecoder[U, T] {
    def decodeStream(u: U): Seq[T]
  }

  def encode[T, U](t: T)(implicit enc: Encoder[T, U]): U =
    try {
      enc.encode(t)
    } catch {
      case t: Throwable =>
        throw new EncodingException(t)
    }

  def decode[U, T](enc: U)(implicit dec: Decoder[U, T]): Option[T] =
    try {
      dec.decode(enc)
    } catch {
      case t: Throwable =>
        throw new DecodingException(t)
    }

  class EncodingException(cause: Throwable) extends RuntimeException(cause)

  class DecodingException(cause: Throwable) extends RuntimeException(cause)

}
