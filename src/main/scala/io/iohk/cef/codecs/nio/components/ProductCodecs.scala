package io.iohk.cef.codecs.nio.components

import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate

import shapeless.{::, Generic, HList, HNil, Lazy}

import scala.reflect.runtime.universe._
import io.iohk.cef.codecs.nio.{NioEncoder, NioDecoder}
import io.iohk.cef.codecs.nio.ops._

trait ProductCodecs {

  implicit val hNilEncoder: NioEncoder[HNil] = (_: HNil) => allocate(0)

  implicit val hNilDecoder: NioDecoder[HNil] = (b: ByteBuffer) => Some(HNil)

  implicit def hListEncoder[H, T <: HList](
      implicit hEncoder: Lazy[NioEncoder[H]],
      tEncoder: Lazy[NioEncoder[T]],
      hlistTT: TypeTag[H :: T]): NioEncoder[H :: T] = {
    new NioEncoder[H :: T] {
      val typeTag: TypeTag[H :: T] = hlistTT
      def encode(l: H :: T): ByteBuffer = l match {
        case h :: t =>
          val hEnc: ByteBuffer = hEncoder.value.encode(h)
          val tEnc: ByteBuffer = tEncoder.value.encode(t)
          allocate(hEnc.capacity() + tEnc.capacity()).put(hEnc).put(tEnc).back
      }
    }

  }

  implicit def hListDecoder[H, T <: HList](
      implicit hDecoder: Lazy[NioDecoder[H]],
      tDecoder: Lazy[NioDecoder[T]],
      hlistTT: TypeTag[H :: T]): NioDecoder[H :: T] = { (b: ByteBuffer) =>
    {
      val initPosition = b.position()

      val r =
        for {
          h <- hDecoder.value.decode(b)
          t <- tDecoder.value.decode(b)
        } yield h :: t

      if (r.isEmpty)
        b.position(initPosition)

      r
    }
  }

  implicit def genericEncoder[T: TypeTag, R](implicit gen: Generic.Aux[T, R], enc: Lazy[NioEncoder[R]]): NioEncoder[T] =
    enc.value.map[T](gen to _).packed

  implicit def genericDecoder[T: TypeTag, R](
      implicit gen: Generic.Aux[T, R],
      dec: Lazy[NioDecoder[R]]): NioDecoder[T] = {
    dec.value.map[T](gen from _).packed
  }
}
