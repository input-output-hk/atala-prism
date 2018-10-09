package io.iohk.cef.encoding.rlp

import akka.util.ByteString
import io.iohk.cef.encoding.{Decoder, Encoder, rlp}

trait RLPEncoders {

  type E[T] = Encoder[T, RLPEncodeable]
  type D[U] = Decoder[RLPEncodeable, U]

  // Adapt concrete RLP encoding scheme to generic cef encoders.
  implicit def genericEnc[T](implicit enc: RLPEncoder[T]): E[T] = enc.encode(_)
  implicit def genericDec[T](implicit dec: RLPDecoder[T]): D[T] = (u: RLPEncodeable) => Some(dec.decode(u))

  //Provide encoders and decoders to ByteString
  val rlpEncodeableEncoder = new Encoder[RLPEncodeable, ByteString] {
    override def encode(t: RLPEncodeable): ByteString =
      ByteString(rlp.rawEncode(t))
  }

  val rlpEncodeableDecoder = new Decoder[ByteString, RLPEncodeable] {
    override def decode(u: ByteString): Option[RLPEncodeable] =
      Some(rlp.rawDecode(u.toArray))
  }

  implicit def byteStringRlpEncoder[T](implicit enc: Encoder[T, RLPEncodeable]): Encoder[T, ByteString] =
    enc.andThen(rlpEncodeableEncoder)

  implicit def byteStringRlpDecoder[U](implicit dec: Decoder[RLPEncodeable, U]): Decoder[ByteString, U] =
    rlpEncodeableDecoder.andThen(dec)

}

object RLPEncoders extends RLPEncoders
