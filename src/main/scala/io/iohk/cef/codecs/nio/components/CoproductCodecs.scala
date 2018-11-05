package io.iohk.cef.codecs.nio.components

import java.nio.ByteBuffer

import shapeless.{:+:, CNil, Coproduct, Inl, Inr, Lazy}

import scala.reflect.runtime.universe._
import io.iohk.cef.codecs.nio.{NioEncoder, NioDecoder}
import io.iohk.cef.codecs.nio.ops._

trait CoproductCodecs {

  implicit val cnilEncoder: NioEncoder[CNil] =
    (n: CNil) => {
      (throw new Exception("Not possible.")): ByteBuffer
    }

  implicit def coproductEncoder[H, T <: Coproduct](
      implicit hEnc: Lazy[NioEncoder[H]],
      tEnc: Lazy[NioEncoder[T]],
      ttc: TypeTag[H :+: T]): NioEncoder[H :+: T] = {
    new NioEncoder[H :+: T] {
      override val typeTag: TypeTag[H :+: T] = ttc
      override def encode(c: H :+: T): ByteBuffer = c match {
        case Inl(h) => hEnc.value.tagged.encode(h)
        case Inr(t) => tEnc.value.encode(t)
      }
    }
  }

  implicit val cnilDecoder: NioDecoder[CNil] =
    (u: ByteBuffer) => {
      None
    }

  implicit def coproductDecoder[H, T <: Coproduct](
      implicit hDec: Lazy[NioDecoder[H]],
      tDec: Lazy[NioDecoder[T]],
      ttc: TypeTag[H :+: T]): NioDecoder[H :+: T] = {
    new NioDecoder[H :+: T] {
      override val typeTag: TypeTag[H :+: T] = ttc
      override def decode(b: ByteBuffer): Option[H :+: T] = {

        val inL: Option[Inl[H, T]] =
          hDec.value.tagged.decode(b).map(h => Inl(h))

        lazy val inR: Option[Inr[H, T]] = tDec.value.decode(b).map(t => Inr(t))

        inL.orElse(inR)
      }
    }
  }

  trait SafeNone[T]

  implicit def optionEncoder[T: NioEncoder]: NioEncoder[Option[T]] = {
    implicit val ttT: TypeTag[T] = NioEncoder[T].typeTag
    val f: Option[T] => ByteBuffer = {
      case s: Some[T] => someEncoder[T].encode(s)
      case None => noneEncoder[T].encode(new SafeNone[T] {})
    }

    f
  }

  private def noneEncoder[T: TypeTag]: NioEncoder[SafeNone[T]] = {
    val inner: NioEncoder[SafeNone[T]] = (_: SafeNone[T]) => ByteBuffer.allocate(0)
    inner.packed
  }

  private def someEncoder[T](implicit enc: NioEncoder[T]): NioEncoder[Some[T]] = {
    implicit val ttT: TypeTag[T] = NioEncoder[T].typeTag
    enc.map[Some[T]](_.get).packed
  }

  implicit def optionDecoder[T: NioDecoder]: NioDecoder[Option[T]] = {
    implicit val ttT: TypeTag[T] = NioDecoder[T].typeTag
    val f: ByteBuffer => Option[Option[T]] =
      (b: ByteBuffer) => {
        val A: Option[Option[T]] = noneDecoder[T].decode(b).map(_ => None)
        def B: Option[Option[T]] = someDecoder[T].decode(b)

        A orElse B
      }

    f
  }

  private def noneDecoder[T: TypeTag]: NioDecoder[SafeNone[T]] = {
    val inner: NioDecoder[SafeNone[T]] = (_: ByteBuffer) => Some(new SafeNone[T] {})
    inner.packed
  }

  private def someDecoder[T](implicit dec: NioDecoder[T]): NioDecoder[Some[T]] = {
    implicit val tt: TypeTag[T] = dec.typeTag
    dec.map(Some.apply).packed
  }

  implicit def eitherEncoder[L: NioEncoder, R: NioEncoder]: NioEncoder[Either[L, R]] = {
    implicit val ttL: TypeTag[L] = NioEncoder[L].typeTag
    implicit val ttR: TypeTag[R] = NioEncoder[R].typeTag

    {
      case l: Left[L, R] => leftEncoder[L, R].encode(l)
      case r: Right[L, R] => rightEncoder[L, R].encode(r)
    }: PartialFunction[Either[L, R], ByteBuffer]
  }

  private def leftEncoder[L: TypeTag, R: TypeTag](implicit lEnc: NioEncoder[L]): NioEncoder[Left[L, R]] =
    lEnc.map[Left[L, R]](_.value).packed

  private def rightEncoder[L: TypeTag, R: TypeTag](implicit rEnc: NioEncoder[R]): NioEncoder[Right[L, R]] =
    rEnc.map[Right[L, R]](_.value).packed

  implicit def eitherDecoder[L: NioDecoder, R: NioDecoder]: NioDecoder[Either[L, R]] = {
    implicit val ttL: TypeTag[L] = NioDecoder[L].typeTag
    implicit val ttR: TypeTag[R] = NioDecoder[R].typeTag

    val f: ByteBuffer => Option[Either[L, R]] =
      (b: ByteBuffer) => {
        val A: Option[Either[L, R]] = leftDecoder[L, R].decode(b)
        def B: Option[Either[L, R]] = rightDecoder[L, R].decode(b)

        A orElse B
      }

    f
  }

  private def leftDecoder[L: TypeTag, R: TypeTag](implicit dec: NioDecoder[L]): NioDecoder[Left[L, R]] =
    dec.map[Left[L, R]](Left.apply).packed

  private def rightDecoder[L: TypeTag, R: TypeTag](implicit dec: NioDecoder[R]): NioDecoder[Right[L, R]] =
    dec.map[Right[L, R]](Right.apply).packed

}

object CoproductCodecs extends CoproductCodecs
