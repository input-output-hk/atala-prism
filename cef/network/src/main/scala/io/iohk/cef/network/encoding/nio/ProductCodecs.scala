package io.iohk.cef.network.encoding.nio

import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate
import java.security.MessageDigest

import scala.reflect.runtime.universe._
import shapeless.{::, Generic, HList, HNil, Typeable}

trait ProductCodecs {

  implicit val hNilEncoder: NioEncoder[HNil] = _ => allocate(0)

  implicit def hListEncoder[H, T <: HList](implicit hEncoder: NioEncoder[H],
                                           tEncoder: NioEncoder[T]): NioEncoder[H :: T] = {
    case h :: t => {
      val hEnc: ByteBuffer = hEncoder.encode(h)
      val tEnc: ByteBuffer = tEncoder.encode(t)
      allocate(hEnc.capacity() + tEnc.capacity()).put(hEnc).put(tEnc).flip().asInstanceOf[ByteBuffer]
    }
  }

  private def hash(s: String): Array[Byte] = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }

  implicit def productEncoder[T, R](implicit gen: Generic.Aux[T, R],
                                    enc: NioEncoder[R],
                                    typeTag: TypeTag[T]): NioEncoder[T] =
    (t: T) => {
      val describe: String = typeTag.toString()
      println(s"description is $describe")
      enc.encode(gen.to(t))
    }

  implicit val hNilDecoder: NioDecoder[HNil] = _ => None

  implicit def hListDecoder[H, T <: HList](implicit hDecoder: NioDecoder[H],
                                           tDecoder: NioDecoder[T],
                                           hDef: Default[H],
                                           tDef: Default[T]): NioDecoder[H :: T] =
    (b: ByteBuffer) => {
      val headOption: Option[H] = hDecoder.decode(b)
      val tailOption: Option[T] = tDecoder.decode(b)
      (headOption, tailOption) match {
        case (Some(h), Some(t)) =>
          Some(h :: t)
        case (Some(h), None) =>
          Some(h :: tDef.zero)
        case _ =>
          None
      }
    }

  implicit def productDecoder[T, R](implicit gen: Generic.Aux[T, R],
                                    dec: NioDecoder[R],
                                    typeInfo: Typeable[T]): NioDecoder[T] =
    (b: ByteBuffer) => {
      dec.decode(b).map(r => gen.from(r))
    }
}
