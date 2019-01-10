package io.iohk.cef.codecs.nio.components

import java.nio.ByteBuffer
import java.util.UUID
import java.time.{Instant, LocalDate}

import akka.util.ByteString

import io.iohk.cef.codecs.nio._
import OtherCodecComponents._

import java.net.{InetAddress, InetSocketAddress}

trait OtherCodecs {

  implicit val bigDecimalCodec: NioCodec[BigDecimal] = NioCodec(bigDecimalEncoder, bigDecimalDecoder)

  implicit def mapCodec[K, V](
      implicit listCodec: NioCodec[List[(K, V)]],
      keyCodec: NioCodec[K],
      valueCodec: NioCodec[V]
  ): NioCodec[Map[K, V]] =
    NioCodec(mapEncoder, mapDecoder)

  implicit def seqCodec[T](implicit arrayCodec: NioCodec[Array[T]], tCodec: NioCodec[T]): NioCodec[Seq[T]] =
    NioCodec(seqEncoder, seqDecoder)

  implicit def listCodec[T](implicit arrayCodec: NioCodec[Array[T]], tCodec: NioCodec[T]): NioCodec[List[T]] =
    NioCodec(listEncoder, listDecoder)

  implicit def setCodec[T](implicit enc: NioCodec[Array[T]], encT: NioCodec[T]): NioCodec[Set[T]] =
    NioCodec(setEncoder, setDecoder)

  implicit val byteStrNioCodec: NioCodec[ByteString] =
    NioCodec(byteStringEncoder, byteStringDecoder)

  implicit val byteBufferCodec: NioCodec[ByteBuffer] =
    NioCodec(byteBufferEncoder, byteBufferDecoder)

  implicit val uuidCodec: NioCodec[UUID] =
    NioCodec(uuidEncoder, uuidDecoder)

  implicit def instantCodec(implicit enc: NioCodec[(Long, Int)]): NioCodec[Instant] =
    NioCodec(instantEncoder, instantDecoder)

  implicit def localDateNioCodec(implicit enc: NioCodec[Instant]): NioCodec[LocalDate] =
    NioCodec(localDateEncoder, localDateDecoder)

  implicit val inetAddressCodec: NioCodec[InetAddress] =
    NioCodec(inetAddressEncoder, inetAddressDecoder)

  implicit def inetSocketAddressCodec(implicit ed: NioCodec[(InetAddress, Int)]): NioCodec[InetSocketAddress] =
    NioCodec(inetSocketAddressEncoder, inetSocketAddressDecoder)
}
