package io.iohk.cef.crypto

import akka.util.ByteString
import io.iohk.cef.codecs.nio.NioCodecs
import io.iohk.cef.codecs.{Decoder => LowLevelDecoder, Encoder => LowLevelEncoder}

trait CryptoCodecs extends NioCodecs {

  type CryptoEncoder[T] = LowLevelEncoder[T, ByteString]
  type CryptoDecoder[T] = LowLevelDecoder[ByteString, T]

  object CryptoEncoder {
    def apply[T](implicit enc: NioEncoder[T]): CryptoEncoder[T] = encoderFromNIOEncoder(enc)
  }

  object CryptoDecoder {
    def apply[T](implicit dec: NioDecoder[T]): CryptoDecoder[T] = decoderFromNIODecoder(dec)
  }

  implicit def encoderFromNIOEncoder[T](implicit nioEncoder: NioEncoder[T]): CryptoEncoder[T] =
    (t: T) => ByteString(nioEncoder.encode(t))

  implicit def decoderFromNIODecoder[T](implicit nioDecoder: NioDecoder[T]): CryptoDecoder[T] =
    (u: ByteString) => nioDecoder.decode(u.toByteBuffer)

  implicit val ByteStringIdentityEncoder: CryptoEncoder[ByteString] = (bs: ByteString) => bs

  implicit val ByteStringIdentityDecoder: CryptoDecoder[ByteString] = (bs: ByteString) => Some(bs)

}
