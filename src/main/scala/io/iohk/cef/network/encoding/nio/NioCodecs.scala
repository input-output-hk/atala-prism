package io.iohk.cef.network.encoding.nio
import java.nio.ByteBuffer

import akka.util.ByteString
import io.iohk.cef.network.encoding._

trait NioCodecs extends NativeCodecs with ProductCodecs with StreamCodecs with OtherCodecs with CoproductCodecs {

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

  object NioCodec {
    def apply[T](implicit enc: NioEncoder[T], dec: NioDecoder[T]): NioCodec[T] = new Codec(enc, dec)
  }

  def nioStreamCodec[T](implicit enc: NioEncoder[T], dec: NioStreamDecoder[T]): NioStreamCodec[T] =
    new NioStreamCodec[T](enc, dec)

  val byteStringNioEncoder: NioEncoder[ByteString] = _.toByteBuffer

  val byteStringNioDecoder: NioDecoder[ByteString] = x => Some(ByteString(x))

  implicit class ByteStringEncoderOps[T](encoder: Encoder[T, ByteString]) {
    def toNioEncoder: NioEncoder[T] = encoder andThen byteStringNioEncoder
  }

  implicit class ByteStringDecoderOps[T](decoder: Decoder[ByteString, T]) {
    def toNioDecoder: NioDecoder[T] = byteStringNioDecoder andThen decoder
  }
}

object NioCodecs extends NioCodecs
