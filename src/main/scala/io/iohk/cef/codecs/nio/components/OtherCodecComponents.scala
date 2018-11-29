package io.iohk.cef.codecs.nio.components

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.components.Ops._
import io.iohk.cef.utils._
import java.nio.ByteBuffer
import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import java.util.UUID

import akka.util.ByteString
import io.iohk.cef.codecs.nio.components.NativeCodecComponents.{
  byteArrayDecoder,
  byteArrayEncoder,
  stringDecoder,
  stringEncoder
}

import scala.util.Try
import scala.reflect.runtime.universe.TypeTag

private[components] object OtherCodecComponents {

  val bigDecimalEncoder: NioEncoder[BigDecimal] =
    stringEncoder.map[BigDecimal](_.toString).packed

  val bigDecimalDecoder: NioDecoder[BigDecimal] =
    stringDecoder.mapOpt[BigDecimal](s => Try(BigDecimal(s)).toOption).packed

  def mapEncoder[K, V](
      implicit enc: NioEncoder[List[(K, V)]],
      encK: NioEncoder[K],
      encV: NioEncoder[V]): NioEncoder[Map[K, V]] = {
    implicit val ttk: TypeTag[K] = encK.typeTag
    implicit val ttv: TypeTag[V] = encV.typeTag
    enc.map[Map[K, V]](_.toList).packed
  }

  def mapDecoder[K, V](
      implicit dec: NioDecoder[List[(K, V)]],
      encK: NioDecoder[K],
      encV: NioDecoder[V]): NioDecoder[Map[K, V]] = {
    implicit val ttk: TypeTag[K] = encK.typeTag
    implicit val ttv: TypeTag[V] = encV.typeTag
    dec.map[Map[K, V]](_.toMap).packed
  }

  def seqEncoder[T](implicit enc: NioEncoder[Array[T]], encT: NioEncoder[T]): NioEncoder[Seq[T]] = {
    implicit val tt: TypeTag[T] = encT.typeTag
    enc.map[Seq[T]](_.toArray).packed
  }

  def seqDecoder[T](implicit dec: NioDecoder[Array[T]], decT: NioDecoder[T]): NioDecoder[Seq[T]] = {
    implicit val tt: TypeTag[T] = decT.typeTag
    dec.map[Seq[T]](_.toSeq).packed
  }

  def listEncoder[T](implicit enc: NioEncoder[Array[T]], encT: NioEncoder[T]): NioEncoder[List[T]] = {
    implicit val tt: TypeTag[T] = encT.typeTag
    enc.map[List[T]](_.toArray).packed
  }

  def listDecoder[T](implicit dec: NioDecoder[Array[T]], decT: NioDecoder[T]): NioDecoder[List[T]] = {
    implicit val tt: TypeTag[T] = decT.typeTag
    dec.map[List[T]](_.toList).packed
  }

  def setEncoder[T](implicit enc: NioEncoder[Array[T]], encT: NioEncoder[T]): NioEncoder[Set[T]] = {
    implicit val tt: TypeTag[T] = encT.typeTag
    enc.map[Set[T]](_.toArray).packed
  }

  def setDecoder[T](implicit dec: NioDecoder[Array[T]], decT: NioDecoder[T]): NioDecoder[Set[T]] = {
    implicit val tt: TypeTag[T] = decT.typeTag
    dec.map[Set[T]](_.toSet).packed
  }

  val byteStringEncoder: NioEncoder[ByteString] =
    byteArrayEncoder.map[ByteString](_.toArray).packed

  val byteStringDecoder: NioDecoder[ByteString] =
    byteArrayDecoder.map[ByteString](ByteString.apply).packed

  val byteBufferEncoder: NioEncoder[ByteBuffer] =
    byteArrayEncoder.map[ByteBuffer]((bb: ByteBuffer) => bb.toArray).packed

  val byteBufferDecoder: NioDecoder[ByteBuffer] =
    byteArrayDecoder.map[ByteBuffer](_.toByteBuffer).packed

  val uuidEncoder: NioEncoder[UUID] =
    stringEncoder.map[UUID](_.toString).packed

  val uuidDecoder: NioDecoder[UUID] =
    stringDecoder.map[UUID](UUID.fromString).packed

  def instantEncoder(implicit enc: NioEncoder[(Long, Int)]): NioEncoder[Instant] =
    enc.map[Instant](i => (i.getEpochSecond, i.getNano)).packed

  def instantDecoder(implicit dec: NioDecoder[(Long, Int)]): NioDecoder[Instant] =
    dec.map { case (l, i) => Instant.ofEpochSecond(l, i.toLong) }.packed

  def localDateEncoder(implicit enc: NioEncoder[Instant]): NioEncoder[LocalDate] =
    enc.map[LocalDate](_.atStartOfDay().toInstant(ZoneOffset.UTC)).packed

  def localDateDecoder(implicit dec: NioDecoder[Instant]): NioDecoder[LocalDate] =
    dec.map[LocalDate](LocalDateTime.ofInstant(_, ZoneOffset.UTC).toLocalDate).packed
}
