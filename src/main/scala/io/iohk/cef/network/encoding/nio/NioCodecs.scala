package io.iohk.cef.network.encoding.nio
import java.nio.ByteBuffer

import akka.util.ByteString
import io.iohk.cef.network.encoding._

trait NioCodecs extends NativeCodecs with GenericCodecs with StreamCodecs {

  type NioEncoder[T] = Encoder[T, ByteBuffer]

  type NioDecoder[T] = Decoder[ByteBuffer, T]

  type NioStreamDecoder[T] = StreamDecoder[ByteBuffer, T]

  type NioCodec[T] = Codec[T, ByteBuffer]

  type NioStreamCodec[T] = StreamCodec[T, ByteBuffer]

  object NioEncoder {
    def apply[T](implicit enc: NioEncoder[T]): NioEncoder[T] = enc
  }

  object NioDecoder {
    def apply[T](implicit dec: NioDecoder[T]): NioDecoder[T] = dec
  }

  def nioStreamCodec[T](implicit enc: NioEncoder[T], dec: NioStreamDecoder[T]): NioStreamCodec[T] =
    new NioStreamCodec[T](enc, dec)

  val byteStringNioEncoder: NioEncoder[ByteString] = new NioEncoder[ByteString] {
    override def encode(t: ByteString): ByteBuffer = t.toByteBuffer
  }

  val byteStringNioDecoder: NioDecoder[ByteString] = new NioDecoder[ByteString] {
    override def decode(u: ByteBuffer): Option[ByteString] = Some(ByteString(u.array()))
  }

  implicit def nioEncoderFromByteStringEncoder[T](implicit encoder: Encoder[T, ByteString]): NioEncoder[T] = {
    encoder andThen byteStringNioEncoder
  }

  implicit def nioDecoderFromByteStringDecoder[T](implicit decoder: Decoder[ByteString, T]): NioDecoder[T] = {
    byteStringNioDecoder andThen decoder
  }
}

object NioCodecs extends NioCodecs
