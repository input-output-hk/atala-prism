package io.iohk.cef.network.encoding.nio

import java.nio.ByteBuffer

import io.iohk.cef.network.encoding.nio.CodecDecorators._

import scala.reflect.runtime.universe._

trait CoproductCodecs {

  implicit def optionEncoder[T: NioEncoder: WeakTypeTag]: NioEncoder[Option[T]] = new NioEncoder[Option[T]] {
    override def encode(o: Option[T]): ByteBuffer =
      messageLengthEncoder(typeCodeEncoder(optionEncoderImpl[T])).encode(o)
  }

  private def optionEncoderImpl[T](implicit enc: NioEncoder[T], t: WeakTypeTag[T]): NioEncoder[Option[T]] =
    new NioEncoder[Option[T]] {
      override def encode(o: Option[T]): ByteBuffer = o.fold(ByteBuffer.allocate(0))(t => enc.encode(t))
    }

  implicit def optionDecoder[T: NioDecoder: WeakTypeTag]: NioDecoder[Option[T]] = new NioDecoder[Option[T]] {
    override def decode(b: ByteBuffer): Option[Option[T]] =
      messageLengthDecoder(typeCodeDecoder(optionDecoderImpl[T])).decode(b)
  }

  private def optionDecoderImpl[T](implicit dec: NioDecoder[T], t: WeakTypeTag[T]): NioDecoder[Option[T]] =
    new NioDecoder[Option[T]] {
      override def decode(b: ByteBuffer): Option[Option[T]] = {
        verifyingSuccess(b)(decodeWithoutUnderflow(dec.decode(b))) match {
          case Some(t) =>
            Some(Some(t))
          case None =>
            None
        }
      }
    }

  implicit def eitherEncoder[L: NioEncoder: WeakTypeTag, R: NioEncoder: WeakTypeTag]: NioEncoder[Either[L, R]] =
    new NioEncoder[Either[L, R]] {
      override def encode(e: Either[L, R]): ByteBuffer = eitherEncoderImpl[L, R].encode(e)
    }

  private def eitherEncoderImpl[L, R](
      implicit lEnc: NioEncoder[L],
      rEnc: NioEncoder[R],
      lt: WeakTypeTag[L],
      rh: WeakTypeTag[R]): NioEncoder[Either[L, R]] = new NioEncoder[Either[L, R]] {
    override def encode(e: Either[L, R]): ByteBuffer = {
      e.fold(l => leftEncoder[L, R].encode(Left(l)), r => rightEncoder[L, R].encode(Right(r)))
    }
  }

  private def leftEncoder[L, R](
      implicit lEnc: NioEncoder[L],
      lt: WeakTypeTag[L],
      rt: WeakTypeTag[R]): NioEncoder[Left[L, R]] =
    new NioEncoder[Left[L, R]] {
      override def encode(l: Left[L, R]): ByteBuffer =
        messageLengthEncoder[Left[L, R]](typeCodeEncoder[Left[L, R]](l => lEnc.encode(l.value))).encode(l)
    }

  private def rightEncoder[L, R](
      implicit rEnc: NioEncoder[R],
      lt: WeakTypeTag[L],
      rt: WeakTypeTag[R]): NioEncoder[Right[L, R]] =
    new NioEncoder[Right[L, R]] {
      override def encode(r: Right[L, R]): ByteBuffer =
        messageLengthEncoder[Right[L, R]](typeCodeEncoder[Right[L, R]](r => rEnc.encode(r.value))).encode(r)
    }

  implicit def eitherDecoder[L: NioDecoder: WeakTypeTag, R: NioDecoder: WeakTypeTag]: NioDecoder[Either[L, R]] =
    new NioDecoder[Either[L, R]] {
      override def decode(b: ByteBuffer): Option[Either[L, R]] =
        eitherDecoderImpl[L, R].decode(b)
    }

  private def eitherDecoderImpl[L, R](
      implicit lDec: NioDecoder[L],
      rDec: NioDecoder[R],
      lt: WeakTypeTag[L],
      rt: WeakTypeTag[R]): NioDecoder[Either[L, R]] = new NioDecoder[Either[L, R]] {
    override def decode(b: ByteBuffer): Option[Either[L, R]] =
      verifyingSuccess(b)(decodeWithoutUnderflow(leftDecoder(lDec, lt, rt).decode(b)))
        .orElse(verifyingSuccess(b)(decodeWithoutUnderflow(rightDecoder(rDec, lt, rt).decode(b))))
  }

  private def leftDecoder[L, R](
      implicit lDec: NioDecoder[L],
      lt: WeakTypeTag[L],
      rt: WeakTypeTag[R]): NioDecoder[Left[L, R]] =
    new NioDecoder[Left[L, R]] {
      override def decode(b: ByteBuffer): Option[Left[L, R]] = {
        messageLengthDecoder[Left[L, R]](typeCodeDecoder[Left[L, R]](u => lDec.decode(u).map(v => Left(v)))).decode(b)
      }
    }

  private def rightDecoder[L, R](
      implicit rDec: NioDecoder[R],
      lt: WeakTypeTag[L],
      rt: WeakTypeTag[R]): NioDecoder[Right[L, R]] =
    new NioDecoder[Right[L, R]] {
      override def decode(b: ByteBuffer): Option[Right[L, R]] = {
        messageLengthDecoder[Right[L, R]](typeCodeDecoder[Right[L, R]](u => rDec.decode(u).map(v => Right(v))))
          .decode(b)
      }
    }
}

object CoproductCodecs extends CoproductCodecs
