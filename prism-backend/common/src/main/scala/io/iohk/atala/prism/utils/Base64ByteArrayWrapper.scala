package io.iohk.atala.prism.utils

import io.circe.{Decoder, Encoder}

import java.util.Base64

// this wrapper helps to ensure that byte array is serialized to Json string using Base64
case class Base64ByteArrayWrapper(value: Array[Byte]) extends AnyVal

object Base64ByteArrayWrapper {
  implicit val arrayByteEncoder: Encoder[Base64ByteArrayWrapper] =
    Encoder.encodeString.contramap[Base64ByteArrayWrapper](wrapper => Base64.getEncoder.encodeToString(wrapper.value))
  implicit val arrayByteDecoder: Decoder[Base64ByteArrayWrapper] =
    Decoder.decodeString.map[Base64ByteArrayWrapper](string => Base64ByteArrayWrapper(Base64.getDecoder.decode(string)))
}
