package io.iohk.cef.test

import akka.util.ByteString
import io.iohk.cef.network.encoding.{Decoder, Encoder}
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.Message

object TestEncoderDecoder {

  case class TestMessage(content: String) extends Message {
    override def code: Int = 1
  }

  val testEncoder: Encoder[String, ByteString] = ByteString(_)

  val testDecoder: Decoder[Message, String] = {
    case TestMessage(content) => Some(content)
    case _ =>
      throw new UnsupportedOperationException(
        s"This is a dummy test decoder and it only supports ${classOf[TestMessage]}")
  }
}
