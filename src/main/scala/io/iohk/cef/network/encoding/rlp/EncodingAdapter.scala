package io.iohk.cef.network.encoding.rlp

import akka.util.ByteString
import io.iohk.cef.encoding.Encoder
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.{Message, MessageSerializable}

class EncodingAdapter[T](message: T, encoder: Encoder[T, ByteString]) extends MessageSerializable {

  private lazy val encodedMessage = encoder.encode(message)

  override def toBytes(implicit di: DummyImplicit): ByteString = encodedMessage

  override def toBytes: Array[Byte] = encodedMessage.toArray

  override def underlyingMsg: Message = new Message {
    override def code: Int = throw new UnsupportedOperationException
  }

  override def code: Int = 1
}
