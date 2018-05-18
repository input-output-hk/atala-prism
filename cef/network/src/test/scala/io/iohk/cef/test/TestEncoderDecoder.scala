package io.iohk.cef.test

import akka.util.ByteString
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.net.transport.rlpx.ethereum.p2p.Message

object TestEncoderDecoder {

  case class TestMessage(content: String) extends Message {
    override def code: Int = 1
  }

  val testEncoder: Encoder[String, ByteString] = ByteString(_)

  val testDecoder: Decoder[Message, String] = {
    case TestMessage(content) => content
    case _ => throw new UnsupportedOperationException(s"This is a dummy test decoder and it only supports ${classOf[TestMessage]}")
  }
}
