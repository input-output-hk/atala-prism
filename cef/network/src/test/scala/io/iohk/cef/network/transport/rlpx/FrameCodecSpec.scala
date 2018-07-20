package io.iohk.cef.network.transport.rlpx

import akka.util.ByteString
import io.iohk.cef.builder.SecureChannelSetup
import io.iohk.cef.encoding.rlp._
import io.iohk.cef.encoding.rlp.{RLPEncodeable, RLPList, RLPSerializable}
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.{Message, MessageSerializable}
import io.iohk.cef.encoding.rlp.RLPImplicits._
import io.iohk.cef.encoding.rlp.RLPImplicitConversions._
import org.scalatest.{FlatSpec, Matchers}

class FrameCodecSpec extends FlatSpec with Matchers {

  import DummyMsg._

  it should "send message and receive a response" in new SecureChannelSetup {
    val frameCodec = new FrameCodec(secrets)
    val remoteFrameCodec = new FrameCodec(remoteSecrets)

    val sampleMessage = DummyMsg(2310, ByteString("Sample Message"))
    val sampleMessageEncoded: ByteString = sampleMessage.toBytes
    val sampleMessageFrame = Frame(Header(sampleMessageEncoded.length, 0, None, Some(sampleMessageEncoded.length)), sampleMessage.code, sampleMessageEncoded)
    val sampleMessageData = remoteFrameCodec.writeFrames(Seq(sampleMessageFrame))
    val sampleMessageReadFrames = frameCodec.readFrames(sampleMessageData)
    val sampleMessageReadMessage = sampleMessageReadFrames.head.payload.toArray[Byte].toSample

    sampleMessageReadMessage shouldBe sampleMessage
  }

  object DummyMsg {
    val code: Int = 2323

    implicit class DummyMsgEnc(val underlyingMsg: DummyMsg) extends MessageSerializable with RLPSerializable {
      override def code: Int = DummyMsg.code

      override def toRLPEncodable: RLPEncodeable = RLPList(underlyingMsg.aField, underlyingMsg.anotherField)
    }

    implicit class DummyMsgDec(val bytes: Array[Byte]) {
      def toSample: DummyMsg = rawDecode(bytes) match {
        case RLPList(aField, anotherField) => DummyMsg(aField, anotherField)
        case _ => throw new RuntimeException("Cannot decode Status")
      }
    }
  }

  case class DummyMsg(aField: Int, anotherField: ByteString) extends Message {
    override def code: Int = DummyMsg.code
  }

}
