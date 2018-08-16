package io.iohk.cef.network.encoding

import java.nio.ByteBuffer

package object nio extends NativeCodecs with GenericCodecs with StreamCodecs {

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
}
