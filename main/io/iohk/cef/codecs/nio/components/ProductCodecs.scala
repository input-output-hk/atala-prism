package io.iohk.cef.codecs.nio.components

import shapeless.{::, Generic, HList, HNil, Lazy}

import scala.reflect.runtime.universe._
import io.iohk.cef.codecs.nio._
import ProductCodecComponents._

trait ProductCodecs {

  implicit val hNilCodec: NioCodec[HNil] = NioCodec(hNilEncoder, hNilDecoder)

  implicit def hListCodec[H, T <: HList](
      implicit hC: Lazy[NioCodec[H]],
      tC: NioCodec[T],
      tt: TypeTag[H :: T]
  ): NioCodec[H :: T] =
    NioCodec(hListEncoder, hListDecoder)

  implicit def genericCodec[T: TypeTag, R](implicit gen: Generic.Aux[T, R], enc: Lazy[NioCodec[R]]): NioCodec[T] =
    NioCodec(genericEncoder, genericDecoder)
}
