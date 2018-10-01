package io.iohk.cef.main

import java.nio.ByteBuffer

import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.network.encoding.array.ArrayCodecs._

object EncoderDecoderSimplificationImplicits {

  type NioEncDec[T] = NioEncoder[T] with NioDecoder[T]

  implicit def nio[T](implicit serializable: ByteStringSerializable[T]): NioEncDec[T] =
    new NioEncoder[T] with NioDecoder[T] {
      val d = serializable.toNioDecoder
      val e = serializable.toNioEncoder
      override def decode(u: ByteBuffer): Option[T] = d.decode(u)
      override def encode(t: T): ByteBuffer = e.encode(t)
    }
}
