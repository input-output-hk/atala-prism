package io.iohk.cef.network.encoding.nio
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import io.iohk.cef.network.encoding.{Codec, Decoder, Encoder, StreamCodec}

trait NioCodecs {

  type NioEncoder[T] = Encoder[T, ByteBuffer]
  type NioDecoder[T] = Decoder[ByteBuffer, T]
  type NioCodec[T] = Codec[T, ByteBuffer]
  type NioStreamCodec[T] = StreamCodec[T, ByteBuffer]

  implicit val stringEncoder: NioEncoder[String] = (s: String) => {
    val b = s.getBytes(StandardCharsets.UTF_8)
    ByteBuffer.allocate(b.length).put(b)
  }

  implicit val stringDecoder: NioDecoder[String] = (u: ByteBuffer) => {
    val p = u.position()
    val l = u.limit()
    val a = new Array[Byte](l - p)
    u.get(a)
    new String(a)
  }

  implicit val intEncoder: NioEncoder[Int] = (i: Int) => ByteBuffer.allocate(4).putInt(i)

  implicit val intDecoder: NioDecoder[Int] = (u: ByteBuffer) => u.getInt()
}

object NioCodecs extends NioCodecs
