package io.iohk.cef.codecs.nio.components

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.components.Ops._

import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate

import shapeless.{::, Generic, HList, HNil, Lazy}

import scala.reflect.runtime.universe.TypeTag

object ProductCodecComponents {

  val hNilEncoder: NioEncoder[HNil] = NioEncoder((_: HNil) => allocate(0))

  val hNilDecoder: NioDecoder[HNil] = NioDecoder((_: ByteBuffer) => Some(HNil))

  def hListEncoder[H, T <: HList](
      implicit hEncoder: Lazy[NioEncoder[H]],
      tEncoder: NioEncoder[T],
      hlistTT: TypeTag[H :: T]): NioEncoder[H :: T] = {
    new NioEncoder[H :: T] {
      val typeTag: TypeTag[H :: T] = hlistTT
      def encode(l: H :: T): ByteBuffer = l match {
        case h :: t =>
          val hEnc: ByteBuffer = hEncoder.value.encode(h)
          val tEnc: ByteBuffer = tEncoder.encode(t)
          allocate(hEnc.capacity() + tEnc.capacity()).put(hEnc).put(tEnc).back()
      }
    }

  }

  def hListDecoder[H, T <: HList](
      implicit hDecoder: Lazy[NioDecoder[H]],
      tDecoder: NioDecoder[T],
      hlistTT: TypeTag[H :: T]): NioDecoder[H :: T] =
    NioDecoder({ b: ByteBuffer =>
      {
        val initPosition = (b: java.nio.Buffer).position()

        val r =
          for {
            h <- hDecoder.value.decode(b)
            t <- tDecoder.decode(b)
          } yield h :: t

        if (r.isEmpty)
          (b: java.nio.Buffer).position(initPosition)

        r
      }
    })

  def genericEncoder[T: TypeTag, R](implicit gen: Generic.Aux[T, R], enc: Lazy[NioEncoder[R]]): NioEncoder[T] =
    enc.value.map[T](gen to _).packed

  def genericDecoder[T: TypeTag, R](implicit gen: Generic.Aux[T, R], dec: Lazy[NioDecoder[R]]): NioDecoder[T] = {
    dec.value.map[T](gen from _).packed
  }
}
