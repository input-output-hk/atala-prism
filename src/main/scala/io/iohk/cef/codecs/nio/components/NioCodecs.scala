package io.iohk.cef.codecs.nio.components
import java.nio.ByteBuffer

import io.iohk.cef.codecs._
import scala.reflect.runtime.universe.TypeTag

trait NioCodecs extends StreamCodecs {

  trait NioCodec[T] extends NioEncoder[T] with NioDecoder[T] with Codec[T, ByteBuffer] { self =>
    def mapOpt[U: TypeTag](ef: U => T, df: T => Option[U]): NioCodec[U] = new NioCodec[U] {
      override def typeTag: TypeTag[U] = implicitly[TypeTag[U]]
      override def encode(u: U): ByteBuffer = self.encode(ef(u))
      override def decode(b: ByteBuffer): Option[U] = self.decode(b).flatMap(df)
    }
    def map[U: TypeTag](ef: U => T, df: T => U): NioCodec[U] =
      mapOpt(ef, (t: T) => Some(df(t)))
  }

  object NioCodec {
    def apply[T](implicit ed: NioCodec[T]): NioCodec[T] = ed
    def apply[T](e: NioEncoder[T], d: NioDecoder[T]): NioCodec[T] =
      new NioCodec[T] {
        override def typeTag: TypeTag[T] = e.typeTag
        override def encode(t: T): ByteBuffer = e.encode(t)
        override def decode(b: ByteBuffer): Option[T] = d.decode(b)
      }
  }

  trait NioEncoder[T] extends Encoder[T, ByteBuffer] { self =>
    def typeTag: TypeTag[T]

    def map[B: TypeTag](f: B => T): NioEncoder[B] =
      NioEncoder((b: B) => self.encode(f(b)))
  }

  object NioEncoder {
    def apply[T](implicit enc: NioEncoder[T]): NioEncoder[T] = enc
    def apply[T: TypeTag](f: T => ByteBuffer): NioEncoder[T] = funcToNioEncoder(f)

    private def funcToNioEncoder[T: TypeTag](f: T => ByteBuffer): NioEncoder[T] =
      new NioEncoder[T] {
        override val typeTag: TypeTag[T] = implicitly[TypeTag[T]]
        override def encode(t: T): ByteBuffer = f(t)
      }
  }

  trait NioDecoder[T] extends Decoder[ByteBuffer, T] { self =>
    def typeTag: TypeTag[T]

    def map[B: TypeTag](f: T => B): NioDecoder[B] =
      NioDecoder((b: ByteBuffer) => self.decode(b).map(f))

    def mapOpt[B: TypeTag](f: T => Option[B]): NioDecoder[B] =
      NioDecoder((b: ByteBuffer) => self.decode(b).flatMap(f))
  }

  object NioDecoder {
    def apply[T](implicit dec: NioDecoder[T]): NioDecoder[T] = dec
    def apply[T: TypeTag](f: ByteBuffer => Option[T]): NioDecoder[T] = funcToNioDecoder(f)

    private def funcToNioDecoder[T: TypeTag](f: ByteBuffer => Option[T]): NioDecoder[T] =
      new NioDecoder[T] {
        override val typeTag: TypeTag[T] = implicitly[TypeTag[T]]
        override def decode(b: ByteBuffer): Option[T] = f(b)
      }
  }

  type NioStreamDecoder[T] = StreamDecoder[ByteBuffer, T]
}
