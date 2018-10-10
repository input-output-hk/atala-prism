package io.iohk.cef.codecs.nio

import java.nio.ByteBuffer
import java.util.UUID

import akka.util.ByteString

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait OtherCodecs {

  implicit val bigDecimalEncoder: NioEncoder[BigDecimal] = new NioEncoder[BigDecimal] {
    override def encode(b: BigDecimal): ByteBuffer = stringEncoder.encode(b.toString())
  }

  implicit val bigDecimalDecoder: NioDecoder[BigDecimal] = new NioDecoder[BigDecimal] {
    override def decode(b: ByteBuffer): Option[BigDecimal] = stringDecoder.decode(b).map(BigDecimal(_))
  }

  implicit def mapEncoder[K, V](implicit enc: NioEncoder[(K, V)]): NioEncoder[Map[K, V]] =
    new NioEncoder[Map[K, V]] {
      override def encode(m: Map[K, V]): ByteBuffer =
        listEncoder[(K, V)].encode(m.toList)
    }

  implicit def mapDecoder[K, V](implicit dec: NioDecoder[(K, V)]): NioDecoder[Map[K, V]] =
    new NioDecoder[Map[K, V]] {
      override def decode(b: ByteBuffer): Option[Map[K, V]] = {
        listDecoder[(K, V)].decode(b).map(_.toMap)
      }
    }

  implicit def seqEncoder[T](implicit enc: NioEncoder[T], tt: WeakTypeTag[T]): NioEncoder[Seq[T]] = {
    implicit val ct: ClassTag[T] = typeToClassTag[T]
    l: Seq[T] =>
      arrayEncoder(enc, tt).encode(l.toArray)
  }

  implicit def seqDecoder[T](implicit dec: NioDecoder[T], tt: WeakTypeTag[T]): NioDecoder[Seq[T]] = {
    implicit val ct: ClassTag[T] = typeToClassTag[T]
    b: ByteBuffer =>
      arrayDecoder(dec, tt).decode(b).map(arr => Seq(arr: _*))
  }

  implicit def listEncoder[T](implicit enc: NioEncoder[T], tt: WeakTypeTag[T]): NioEncoder[List[T]] = {
    implicit val ct: ClassTag[T] = typeToClassTag[T]
    l: List[T] =>
      arrayEncoder(enc, tt).encode(l.toArray)
  }

  implicit def listDecoder[T](implicit dec: NioDecoder[T], tt: WeakTypeTag[T]): NioDecoder[List[T]] = {
    implicit val ct: ClassTag[T] = typeToClassTag[T]
    b: ByteBuffer =>
      arrayDecoder(dec, tt).decode(b).map(arr => List(arr: _*))
  }

  implicit val byteStringEncoder: NioEncoder[ByteString] = new NioEncoder[ByteString] {
    override def encode(b: ByteString): ByteBuffer =
      arrayEncoder(byteEncoder, implicitly[WeakTypeTag[Byte]]).encode(b.toArray)
  }

  implicit val byteStringDecoder: NioDecoder[ByteString] = new NioDecoder[ByteString] {
    override def decode(b: ByteBuffer): Option[ByteString] =
      arrayDecoder(byteDecoder, implicitly[WeakTypeTag[Byte]]).decode(b).map(arr => ByteString(arr))
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
