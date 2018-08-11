package io.iohk.cef.network.encoding.nio
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import io.iohk.cef.network.encoding.{Decoder, Encoder}

trait NioCodecs {

  implicit val stringEncoder = new Encoder[String, ByteBuffer] {
    override def encode(s: String): ByteBuffer = {
      val b = s.getBytes(StandardCharsets.UTF_8)
      ByteBuffer.allocate(b.length).put(b)
    }
  }

  implicit val stringDecoder = new Decoder[ByteBuffer, String] {
    override def decode(u: ByteBuffer): String = {
      val p = u.position()
      val l = u.limit()
      val a = new Array[Byte](l - p)
      u.get(a)
      new String(a)
    }
  }

}

object NioCodecs extends NioCodecs
