package io.iohk.cef.codecs.nio.components
import java.nio.ByteBuffer

import io.iohk.cef.codecs._
import scala.reflect.runtime.universe.TypeTag

trait NioCodecs extends Extensions with StreamCodecs {

  object auto extends NativeCodecs with ProductCodecs with OtherCodecs with CoproductCodecs

  trait NioEncDec[T] extends NioEncoder[T] with NioDecoder[T] with EncoderDecoder[T, ByteBuffer]
  object NioEncDec {
    def apply[T](implicit ed: NioEncDec[T]): NioEncDec[T] = ed
    def apply[T](e: NioEncoder[T], d: NioDecoder[T]): NioEncDec[T] =
      new NioEncDec[T] {
        override val typeTag: TypeTag[T] = e.typeTag
        override def encode(t: T): ByteBuffer = e.encode(t)
        override def decode(b: ByteBuffer): Option[T] = d.decode(b)
      }
    implicit def NioEncDecFromEncoderAndDecoder[T](implicit e: NioEncoder[T], d: NioDecoder[T]): NioEncDec[T] =
      apply[T](e, d)
  }

  trait NioEncoder[T] extends Encoder[T, ByteBuffer] { self =>
    val typeTag: TypeTag[T]

    def map[B: TypeTag](f: B => T): NioEncoder[B] =
      (b: B) => self.encode(f(b))
  }
  object NioEncoder {
    import scala.language.implicitConversions

    def apply[T](implicit enc: NioEncoder[T]): NioEncoder[T] = enc
    def apply[T: TypeTag](f: T => ByteBuffer): NioEncoder[T] = funcToNioEncoder(f)

    implicit def funcToNioEncoder[T: TypeTag](f: T => ByteBuffer): NioEncoder[T] =
      new NioEncoder[T] {
        override val typeTag: TypeTag[T] = implicitly[TypeTag[T]]
        override def encode(t: T): ByteBuffer = f(t)
      }
  }

  trait NioDecoder[T] extends Decoder[ByteBuffer, T] { self =>
    val typeTag: TypeTag[T]

    def map[B: TypeTag](f: T => B): NioDecoder[B] =
      (b: ByteBuffer) => self.decode(b).map(f)

    def mapOpt[B: TypeTag](f: T => Option[B]): NioDecoder[B] =
      (b: ByteBuffer) => self.decode(b).flatMap(f)
  }
  object NioDecoder {
    import scala.language.implicitConversions

    def apply[T](implicit dec: NioDecoder[T]): NioDecoder[T] = dec
    def apply[T: TypeTag](f: ByteBuffer => Option[T]): NioDecoder[T] = funcToNioDecoder(f)

    implicit def funcToNioDecoder[T: TypeTag](f: ByteBuffer => Option[T]): NioDecoder[T] =
      new NioDecoder[T] {
        override val typeTag: TypeTag[T] = implicitly[TypeTag[T]]
        override def decode(b: ByteBuffer): Option[T] = f(b)
      }
  }

  type NioStreamDecoder[T] = StreamDecoder[ByteBuffer, T]

  type NioCodec[T] = Codec[T, ByteBuffer]

  type NioStreamCodec[T] = StreamCodec[T, ByteBuffer]

  object NioCodec {
    def apply[T](implicit enc: NioEncoder[T], dec: NioDecoder[T]): NioCodec[T] = new Codec(enc, dec)
  }

  def nioStreamCodec[T](implicit enc: NioEncoder[T], dec: NioStreamDecoder[T]): NioStreamCodec[T] =
    new NioStreamCodec[T](enc, dec)
}
