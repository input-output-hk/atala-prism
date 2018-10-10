package io.iohk.cef.codecs.nio

import java.nio.ByteBuffer

import io.iohk.cef.codecs.nio.CodecDecorators._
import shapeless.{:+:, CNil, Coproduct, Inl, Inr, Lazy}

import scala.reflect.runtime.universe._

trait CoproductCodecs {

  implicit val cnilEncoder: NioEncoder[CNil] = new NioEncoder[CNil] {
    override def encode(t: CNil): ByteBuffer = throw new Exception("Not possible.")
  }

  implicit def coproductEncoder[H, T <: Coproduct](
      implicit hEnc: Lazy[NioEncoder[H]],
      tEnc: NioEncoder[T]): NioEncoder[H :+: T] = new NioEncoder[H :+: T] {
    override def encode(c: H :+: T): ByteBuffer = c match {
      case Inl(h) => hEnc.value.encode(h)
      case Inr(t) => tEnc.encode(t)
    }
  }

  implicit val cnilDecoder: NioDecoder[CNil] = new NioDecoder[CNil] {
    override def decode(u: ByteBuffer): Option[CNil] = None
  }

  implicit def coproductDecoder[H, T <: Coproduct](
      implicit hDec: Lazy[NioDecoder[H]],
      tDec: NioDecoder[T]): NioDecoder[H :+: T] = new NioDecoder[H :+: T] {
    override def decode(b: ByteBuffer): Option[H :+: T] = {
      val inL: Option[Inl[H, T]] = hDec.value.decode(b).map(h => Inl(h))

      lazy val inR: Option[Inr[H, T]] = tDec.decode(b).map(t => Inr(t))

      inL.orElse(inR)
    }
  }

  trait SafeNone[T]

  implicit def optionEncoder[T: NioEncoder: WeakTypeTag]: NioEncoder[Option[T]] = new NioEncoder[Option[T]] {
    override def encode(o: Option[T]): ByteBuffer = o match {
      case s @ Some(_) =>
        someEncoder[T].encode(s)
      case None =>
        noneEncoder.encode(new SafeNone[T] {})
    }
  }

  private def noneEncoder[T: WeakTypeTag]: NioEncoder[SafeNone[T]] = new NioEncoder[SafeNone[T]] {
    override def encode(t: SafeNone[T]): ByteBuffer =
      messageLengthEncoder[SafeNone[T]](typeCodeEncoder[SafeNone[T]](_ => ByteBuffer.allocate(0))).encode(t)
  }

  private def someEncoder[T](implicit enc: NioEncoder[T], tt: WeakTypeTag[T]): NioEncoder[Some[T]] =
    new NioEncoder[Some[T]] {
      override def encode(t: Some[T]): ByteBuffer =
        messageLengthEncoder[Some[T]](typeCodeEncoder[Some[T]](v => enc.encode(v.get))).encode(t)
    }

  implicit def optionDecoder[T: NioDecoder: WeakTypeTag]: NioDecoder[Option[T]] = new NioDecoder[Option[T]] {
    override def decode(b: ByteBuffer): Option[Option[T]] =
      verifyingSuccess(b)(decodeWithoutUnderflow[Option[T]](noneDecoder.decode(b).map((sn: SafeNone[T]) => None)))
        .orElse(verifyingSuccess(b)(decodeWithoutUnderflow(someDecoder[T].decode(b))))
        .orElse(None)
  }

  private def noneDecoder[T: WeakTypeTag]: NioDecoder[SafeNone[T]] = new NioDecoder[SafeNone[T]] {
    override def decode(b: ByteBuffer): Option[SafeNone[T]] =
      messageLengthDecoder[SafeNone[T]](typeCodeDecoder[SafeNone[T]](_ => Some(new SafeNone[T] {}))).decode(b)
  }

  private def someDecoder[T: WeakTypeTag](implicit dec: NioDecoder[T]): NioDecoder[Some[T]] = new NioDecoder[Some[T]] {
    override def decode(b: ByteBuffer): Option[Some[T]] =
      messageLengthDecoder[Some[T]](typeCodeDecoder[Some[T]](u => dec.decode(u).map(v => Some(v)))).decode(b)
  }

  implicit def eitherEncoder[L: NioEncoder: WeakTypeTag, R: NioEncoder: WeakTypeTag]: NioEncoder[Either[L, R]] =
    new NioEncoder[Either[L, R]] {
      override def encode(e: Either[L, R]): ByteBuffer =
        e.fold(l => leftEncoder[L, R].encode(Left(l)), r => rightEncoder[L, R].encode(Right(r)))
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

  implicit def eitherDecoder[L, R](
      implicit lDec: NioDecoder[L],
      rDec: NioDecoder[R],
      lt: WeakTypeTag[L],
      rt: WeakTypeTag[R]): NioDecoder[Either[L, R]] =
    new NioDecoder[Either[L, R]] {
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
