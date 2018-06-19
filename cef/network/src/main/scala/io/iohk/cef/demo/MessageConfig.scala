package io.iohk.cef.demo

import akka.util.ByteString
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.Message.Version
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.{Message, MessageDecoder, MessageSerializable}

object MessageConfig {

  case class SampleMessage(content: String) extends MessageSerializable {
    override def toBytes(implicit di: DummyImplicit): ByteString = ByteString(content)

    override def toBytes: Array[Byte] = content.getBytes

    override def underlyingMsg: Message = this

    override def code: Version = 1
  }

  val sampleMessageDecoder = new MessageDecoder {
    override def fromBytes(`type`: Int, payload: Array[Byte], protocolVersion: Version): Message = SampleMessage(new String(payload))
  }

  val sampleEncoder: Encoder[String, ByteString] = ByteString(_)

  val sampleDecoder: Decoder[Message, String] = {
    case SampleMessage(content) => content
    case _ => throw new UnsupportedOperationException(s"This is a dummy test decoder and it only supports ${classOf[SampleMessage]}")
  }
}
