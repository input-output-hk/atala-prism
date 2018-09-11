package io.iohk.cef.network.encoding.array
import java.nio.ByteBuffer

import io.iohk.cef.network.encoding._
import io.iohk.cef.network.encoding.nio.NioCodecs

trait ArrayCodecs extends NioCodecs {

  type ArrayEncoder[T] = Encoder[T, Array[Byte]]

  type ArrayDecoder[T] = Decoder[Array[Byte], T]

  type ArrayCodec[T] = Codec[T, Array[Byte]]

  object ArrayEncoder {
    def apply[T](implicit enc: ArrayEncoder[T]): ArrayEncoder[T] = enc
  }

  object ArrayDecoder {
    def apply[T](implicit dec: ArrayDecoder[T]): ArrayDecoder[T] = dec
  }

  implicit def arrayEncoder[T](implicit nioEncoder: NioEncoder[T]): ArrayEncoder[T] = new ArrayEncoder[T] {
    override def encode(t: T): Array[Byte] = {
      val byteBuffer = nioEncoder.encode(t)
      val array = new Array[Byte](byteBuffer.remaining())
      Range(0, byteBuffer.remaining()).foreach(i => array(i) = byteBuffer.get())
      array
    }
  }

  implicit def arrayDecoder[T](implicit nioDecoder: NioDecoder[T]): ArrayDecoder[T] = new ArrayDecoder[T] {
    override def decode(array: Array[Byte]): Option[T] = {
      val byteBuffer = ByteBuffer.allocate(array.length)
      array.foreach(b => byteBuffer.put(b))
      byteBuffer.flip()
      nioDecoder.decode(byteBuffer)
    }
  }
}

object ArrayCodecs extends ArrayCodecs
