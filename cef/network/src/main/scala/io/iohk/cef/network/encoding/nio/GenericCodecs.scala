package io.iohk.cef.network.encoding.nio

import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate
import java.security.MessageDigest

import shapeless.{::, Generic, HList, HNil}
import scala.reflect.ClassTag

trait GenericCodecs {

  implicit val hNilEncoder: NioEncoder[HNil] = _ => allocate(0)

  implicit val hNilDecoder: NioDecoder[HNil] = _ => None

  implicit def hListEncoder[H, T <: HList](implicit hEncoder: NioEncoder[H],
                                           tEncoder: NioEncoder[T]): NioEncoder[H :: T] = {
    case h :: t => {
      val hEnc: ByteBuffer = hEncoder.encode(h)
      val tEnc: ByteBuffer = tEncoder.encode(t)
      allocate(hEnc.capacity() + tEnc.capacity()).put(hEnc).put(tEnc).flip().asInstanceOf[ByteBuffer]
    }
  }

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

  implicit def genericEncoder[T, R](implicit gen: Generic.Aux[T, R],
                                    enc: NioEncoder[R],
                                    ct: ClassTag[T]): NioEncoder[T] = new NioEncoder[T] {
    override def encode(t: T): ByteBuffer = {
      val hashBuff: ByteBuffer = arrayEncoder[Byte].encode(hash(ct.runtimeClass.getName))
      val messageBuff: ByteBuffer = enc.encode(gen.to(t))

      ByteBuffer
        .allocate(hashBuff.capacity() + messageBuff.capacity())
        .put(hashBuff)
        .put(messageBuff)
        .flip()
        .asInstanceOf[ByteBuffer]
    }
  }

  implicit def genericDecoder[T, R](implicit gen: Generic.Aux[T, R],
                                    dec: NioDecoder[R],
                                    ct: ClassTag[T]): NioDecoder[T] =
    new NioDecoder[T] {
      override def decode(b: ByteBuffer): Option[T] = {

        val initialPosition = b.position()
        val expectedTypeHash: Array[Byte] = hash(ct.runtimeClass.getName)
        val actualTypeHashDec: Option[Array[Byte]] = arrayDecoder[Byte].decode(b)

        val matchingTypeHash = actualTypeHashDec
          .exists(actualTypeHash => actualTypeHash.deep == expectedTypeHash.deep)

        if (matchingTypeHash)
          dec.decode(b).map(gen.from)
        else {
          b.position(initialPosition)
          None
        }
      }
    }

  private def hash(s: String): Array[Byte] = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }
}
