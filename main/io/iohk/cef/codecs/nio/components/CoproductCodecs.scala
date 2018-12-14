package io.iohk.cef.codecs.nio.components

import shapeless.{:+:, CNil, Coproduct, Lazy}

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.components.CoproductCodecComponents._

import scala.reflect.runtime.universe._

trait CoproductCodecs {

  implicit val cNilCodec: NioCodec[CNil] = NioCodec(cnilEncoder, cnilDecoder)

  implicit def coproductCodec[H, T <: Coproduct](
      implicit hC: Lazy[NioCodec[H]],
      tC: NioCodec[T],
      ttc: TypeTag[H :+: T]): NioCodec[H :+: T] =
    NioCodec(coproductEncoder, coproductDecoder)

  implicit def optionCodec[T: NioCodec]: NioCodec[Option[T]] =
    NioCodec(optionEncoder, optionDecoder)

  implicit def eitherCodec[L: NioCodec, R: NioCodec]: NioCodec[Either[L, R]] =
    NioCodec(eitherEncoder, eitherDecoder)

}
