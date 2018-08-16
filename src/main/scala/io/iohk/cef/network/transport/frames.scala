package io.iohk.cef.network.transport
import java.nio.ByteBuffer

import io.iohk.cef.network.NodeId
import io.iohk.cef.network.encoding.{Decoder, Encoder, StreamDecoder}
import io.iohk.cef.network.transport.FrameHeader._

object FrameHeader {
  val defaultTtl = 5
  val ttlLength: Int = 4
  val frameHeaderLength: Int = 4 + NodeId.nodeIdBytes + NodeId.nodeIdBytes + ttlLength
}

case class FrameHeader(src: NodeId, dst: NodeId, ttl: Int = defaultTtl)

case class Frame[Message](header: FrameHeader, content: Message)


class FrameEncoder[Message](messageEncoder: Encoder[Message, ByteBuffer]) extends Encoder[Frame[Message], ByteBuffer] {

  private def length(h: FrameHeader, userMessageBuffer: ByteBuffer): Int =
    frameHeaderLength + userMessageBuffer.position(0).remaining()

  override def encode(f: Frame[Message]): ByteBuffer = {
    val userMessageBuffer = messageEncoder.encode(f.content)
    val l = length(f.header, userMessageBuffer)
    val b = ByteBuffer.allocate(l)
    b.putInt(l)
      .put(f.header.src.id.toByteBuffer)
      .put(f.header.dst.id.toByteBuffer)
      .putInt(f.header.ttl)
      .put(userMessageBuffer)
      .flip()
      .asInstanceOf[ByteBuffer]
  }
}

class FrameDecoder[Message](messageDecoder: Decoder[ByteBuffer, Message])
    extends StreamDecoder[ByteBuffer, Frame[Message]] {

  override def decodeStream(b: ByteBuffer): Seq[Frame[Message]] = {
    @annotation.tailrec
    def loop(acc: Seq[Frame[Message]]): Seq[Frame[Message]] = {
      decodeFrame(b) match {
        case None        => acc
        case Some(frame) => loop(acc :+ frame)
      }
    }
    loop(Vector())
  }

  private def decodeFrame(b: ByteBuffer): Option[Frame[Message]] = {
    val p0 = b.position()
    val remaining = b.remaining()
    if (remaining > 4) {
      val messageLength = b.getInt()
      if (remaining >= messageLength) {
        val src = decodeNodeId(b)
        val dst = decodeNodeId(b)
        val ttl = b.getInt()
        val content = decodeUserMessage(messageLength, b)

        Some(Frame[Message](FrameHeader(src, dst, ttl), content))
      } else {
        b.position(p0)
        None
      }
    } else {
      None
    }
  }

  private def decodeUserMessage(messageLength: Int, b: ByteBuffer): Message = {
    val currentLimit = b.limit()
    val userMessageLength = messageLength - frameHeaderLength
    val currentPos = b.position()
    try {
      messageDecoder.decode(b.limit(currentPos + userMessageLength).asInstanceOf[ByteBuffer])
    } finally {
      b.limit(currentLimit)
    }
  }

  private def decodeNodeId(b: ByteBuffer): NodeId = {
    val a = new Array[Byte](NodeId.nodeIdBytes)
    b.get(a)
    NodeId(a)
  }
}
