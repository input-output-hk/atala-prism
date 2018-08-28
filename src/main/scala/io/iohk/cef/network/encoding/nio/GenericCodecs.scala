package io.iohk.cef.network.encoding.nio

import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate

import io.iohk.cef.network.encoding.nio.CodecDecorators._
import shapeless.{::, Generic, HList, HNil, Lazy}

import scala.reflect.ClassTag

trait GenericCodecs {

  implicit val hNilEncoder: NioEncoder[HNil] = _ => allocate(0)

  implicit val hNilDecoder: NioDecoder[HNil] = _ => Some(HNil)

  implicit def hListEncoder[H, T <: HList](implicit hEncoder: Lazy[NioEncoder[H]],
                                           tEncoder: NioEncoder[T]): NioEncoder[H :: T] = {
    case h :: t => {
      val hEnc: ByteBuffer = hEncoder.value.encode(h)
      val tEnc: ByteBuffer = tEncoder.encode(t)
      allocate(hEnc.capacity() + tEnc.capacity()).put(hEnc).put(tEnc).flip().asInstanceOf[ByteBuffer]
    }
  }

  implicit def hListDecoder[H, T <: HList](implicit hDecoder: Lazy[NioDecoder[H]],
                                           tDecoder: NioDecoder[T]): NioDecoder[H :: T] =
    (b: ByteBuffer) => {
      val initPosition = b.position()
      val headOption: Option[H] = hDecoder.value.decode(b)
      val tailOption: Option[T] = tDecoder.decode(b)
      (headOption, tailOption) match {
        case (Some(h), Some(t)) =>
          Some(h :: t)
        case _ =>
          b.position(initPosition)
          None
      }
    }

  implicit def genericEncoder[T, R](implicit gen: Generic.Aux[T, R],
                                    enc: Lazy[NioEncoder[R]],
                                    ct: ClassTag[T]): NioEncoder[T] =
    messageLengthEncoder(typeCodeEncoder(t => enc.value.encode(gen.to(t))))

  implicit def genericDecoder[T, R](implicit gen: Generic.Aux[T, R],
                                    dec: Lazy[NioDecoder[R]],
                                    ct: ClassTag[T]): NioDecoder[T] =
    messageLengthDecoder(typeCodeDecoder((b: ByteBuffer) => dec.value.decode(b).map(gen.from)))
}
