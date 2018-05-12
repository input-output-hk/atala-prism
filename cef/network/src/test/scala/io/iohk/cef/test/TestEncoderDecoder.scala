package io.iohk.cef.test

import io.iohk.cef.encoding.{Decoder, Encoder}

object TestEncoderDecoder {

  val testEncoder: Encoder[String, Array[Byte]] = _.getBytes
  val testDecoder: Decoder[Array[Byte], String] = new String(_)

}
