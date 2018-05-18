package io.iohk.cef.test

import akka.util.ByteString
import io.iohk.cef.net.transport.rlpx.ethereum.p2p.{Message, MessageSerializable}

case class DummyMessage(msg: String) extends MessageSerializable {

  override def toBytes(implicit di: DummyImplicit): ByteString = ByteString(msg.getBytes)

  override def toBytes: Array[Byte] = msg.getBytes

  override def underlyingMsg: Message = new Message {
    override def code: Int = -1
  }

  override def code: Int = -1
}
