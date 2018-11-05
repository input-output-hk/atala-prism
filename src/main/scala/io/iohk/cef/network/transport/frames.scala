package io.iohk.cef.network.transport

import io.iohk.cef.network.NodeId
import io.iohk.cef.codecs.nio.{NioEncoder, NioDecoder, NioEncDec}
import scala.reflect.runtime.universe.TypeTag

object FrameHeader {
  val defaultTtl = 5
  val ttlLength: Int = 4
  val frameHeaderLength: Int = 4 + NodeId.nodeIdBytes + NodeId.nodeIdBytes + ttlLength

  def apply(src: NodeId, dst: NodeId): FrameHeader = FrameHeader(src, dst, defaultTtl)
}

case class FrameHeader(src: NodeId, dst: NodeId, ttl: Int)

case class Frame[Message](header: FrameHeader, content: Message)
object Frame {
  implicit def FrameEncDec[M: NioEncDec]: NioEncDec[Frame[M]] = {
    import io.iohk.cef.codecs.nio.auto._
    implicit val ttt: TypeTag[M] = NioEncDec[M].typeTag
    val e: NioEncoder[Frame[M]] = genericEncoder
    val d: NioDecoder[Frame[M]] = genericDecoder
    NioEncDec(e, d)
  }
}
