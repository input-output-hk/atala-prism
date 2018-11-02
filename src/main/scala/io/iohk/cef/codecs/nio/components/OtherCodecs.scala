package io.iohk.cef.codecs.nio.components

import java.nio.ByteBuffer
import java.util.UUID
import java.time.Instant

import akka.util.ByteString

import scala.reflect.runtime.universe.TypeTag
import io.iohk.cef.utils._
import scala.util.Try
import io.iohk.cef.codecs.nio.{NioEncoder, NioDecoder}
import io.iohk.cef.codecs.nio.ops._

trait OtherCodecs {

  implicit def bigDecimalEncoder(implicit se: NioEncoder[String]): NioEncoder[BigDecimal] =
    se.map[BigDecimal](_.toString).packed

  implicit def bigDecimalDecoder(implicit sd: NioDecoder[String]): NioDecoder[BigDecimal] =
    sd.mapOpt[BigDecimal](s => Try(BigDecimal(s)).toOption).packed

  implicit def mapEncoder[K, V](
      implicit enc: NioEncoder[List[(K, V)]],
      encK: NioEncoder[K],
      encV: NioEncoder[V]): NioEncoder[Map[K, V]] = {
    implicit val ttk: TypeTag[K] = encK.typeTag
    implicit val ttv: TypeTag[V] = encV.typeTag
    enc.map[Map[K, V]](_.toList).packed
  }

  implicit def mapDecoder[K, V](
      implicit dec: NioDecoder[List[(K, V)]],
      encK: NioDecoder[K],
      encV: NioDecoder[V]): NioDecoder[Map[K, V]] = {
    implicit val ttk: TypeTag[K] = encK.typeTag
    implicit val ttv: TypeTag[V] = encV.typeTag
    dec.map[Map[K, V]](_.toMap).packed
  }

  implicit def seqEncoder[T](implicit enc: NioEncoder[Array[T]], encT: NioEncoder[T]): NioEncoder[Seq[T]] = {
    implicit val tt: TypeTag[T] = encT.typeTag
    enc.map[Seq[T]](_.toArray).packed
  }

  implicit def seqDecoder[T](implicit dec: NioDecoder[Array[T]], decT: NioDecoder[T]): NioDecoder[Seq[T]] = {
    implicit val tt: TypeTag[T] = decT.typeTag
    dec.map[Seq[T]](_.toSeq).packed
  }

  implicit def listEncoder[T](implicit enc: NioEncoder[Array[T]], encT: NioEncoder[T]): NioEncoder[List[T]] = {
    implicit val tt: TypeTag[T] = encT.typeTag
    enc.map[List[T]](_.toArray).packed
  }

  implicit def listDecoder[T](implicit dec: NioDecoder[Array[T]], decT: NioDecoder[T]): NioDecoder[List[T]] = {
    implicit val tt: TypeTag[T] = decT.typeTag
    dec.map[List[T]](_.toList).packed
  }

  implicit def setEncoder[T](implicit enc: NioEncoder[Array[T]], encT: NioEncoder[T]): NioEncoder[Set[T]] = {
    implicit val tt: TypeTag[T] = encT.typeTag
    enc.map[Set[T]](_.toArray).packed
  }

  implicit def setDecoder[T](implicit dec: NioDecoder[Array[T]], decT: NioDecoder[T]): NioDecoder[Set[T]] = {
    implicit val tt: TypeTag[T] = decT.typeTag
    dec.map[Set[T]](_.toSet).packed
  }

  implicit def byteStringEncoder(implicit enc: NioEncoder[Array[Byte]]): NioEncoder[ByteString] =
    enc.map[ByteString](_.toArray).packed

  implicit def byteStringDecoder(implicit dec: NioDecoder[Array[Byte]]): NioDecoder[ByteString] =
    dec.map[ByteString](ByteString.apply).packed

  implicit def byteBufferEncoder(implicit enc: NioEncoder[Array[Byte]]): NioEncoder[ByteBuffer] =
    enc.map[ByteBuffer](_.toArray).packed

  implicit def byteBufferDecoder(implicit dec: NioDecoder[Array[Byte]]): NioDecoder[ByteBuffer] =
    dec.map[ByteBuffer](_.toByteBuffer).packed

  implicit def uuidEncoder(implicit enc: NioEncoder[String]): NioEncoder[UUID] =
    enc.map[UUID](_.toString).packed

  implicit def uuidDecoder(implicit dec: NioDecoder[String]): NioDecoder[UUID] =
    dec.map[UUID](UUID.fromString).packed

  implicit def instantEncoder(implicit enc: NioEncoder[(Long, Int)]): NioEncoder[Instant] =
    enc.map[Instant](i => (i.getEpochSecond, i.getNano)).packed

  implicit def instantDecoder(implicit dec: NioDecoder[(Long, Int)]): NioDecoder[Instant] =
    dec.map { case (l, i) => Instant.ofEpochSecond(l, i.toLong) }.packed

}
