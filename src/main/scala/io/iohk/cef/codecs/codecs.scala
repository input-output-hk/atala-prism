package io.iohk.cef

package object codecs {

  trait Codec[T, U] extends Encoder[T, U] with Decoder[U, T] { self =>
    def andThen[V](thatCodec: Codec[U, V]): Codec[T, V] = {
      new Codec[T, V] {
        override def encode(t: T): V =
          thatCodec.encode(self.encode(t))
        override def decode(v: V): Option[T] = {
          thatCodec.decode(v).flatMap(u => self.decode(u))
        }
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
}
