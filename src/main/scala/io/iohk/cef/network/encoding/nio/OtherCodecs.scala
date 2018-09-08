package io.iohk.cef.network.encoding.nio

import java.nio.ByteBuffer
import java.util.UUID

import akka.util.ByteString

import scala.reflect.ClassTag

trait OtherCodecs {

  implicit def listEncoder[T](implicit enc: NioEncoder[T], ct: ClassTag[T]): NioEncoder[List[T]] =
    (l: List[T]) => arrayEncoder(enc, ct).encode(l.toArray)

  implicit def listDecoder[T](implicit dec: NioDecoder[T], ct: ClassTag[T]): NioDecoder[List[T]] =
    (b: ByteBuffer) => arrayDecoder(dec, ct).decode(b).map(arr => List(arr: _*))

  implicit val byteStringEncoder: NioEncoder[ByteString] = new NioEncoder[ByteString] {
    override def encode(b: ByteString): ByteBuffer =
      arrayEncoder(byteEncoder, ClassTag(classOf[Byte])).encode(b.toArray)
  }

  implicit val byteStringDecoder: NioDecoder[ByteString] = new NioDecoder[ByteString] {
    override def decode(b: ByteBuffer): Option[ByteString] =
      arrayDecoder(byteDecoder, ClassTag(classOf[Byte])).decode(b).map(arr => ByteString(arr))
  }

  implicit val uuidEncoder: NioEncoder[UUID] = new NioEncoder[UUID] {
    override def encode(u: UUID): ByteBuffer = stringEncoder.encode(u.toString)
  }

  implicit val uuidDecoder: NioDecoder[UUID] = new NioDecoder[UUID] {
    override def decode(u: ByteBuffer): Option[UUID] = {
      stringDecoder.decode(u).map(s => UUID.fromString(s))
    }
  }
}

object OtherCodecs extends OtherCodecs
