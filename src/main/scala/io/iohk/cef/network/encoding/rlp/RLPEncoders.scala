package io.iohk.cef.network.encoding.rlp

import io.iohk.cef.codecs.{Decoder, Encoder}

trait RLPEncoders {

  type E[T] = Encoder[T, RLPEncodeable]
  type D[U] = Decoder[RLPEncodeable, U]

  // Adapt concrete RLP encoding scheme to generic cef encoders.
  implicit def genericEnc[T](implicit enc: RLPEncoder[T]): E[T] = enc.encode(_)
  implicit def genericDec[T](implicit dec: RLPDecoder[T]): D[T] = (u: RLPEncodeable) => Some(dec.decode(u))
}

object RLPEncoders extends RLPEncoders
