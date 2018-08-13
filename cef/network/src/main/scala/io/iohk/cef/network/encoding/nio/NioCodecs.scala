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

  implicit val intEncoder = new Encoder[Int, ByteBuffer] {
    override def encode(i: Int): ByteBuffer =
      ByteBuffer.allocate(4).putInt(i)
  }

  implicit val intDecoder = new Decoder[ByteBuffer, Int] {
    override def decode(u: ByteBuffer): Int =
      u.getInt()
  }


}

object NioCodecs extends NioCodecs
