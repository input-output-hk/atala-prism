package io.iohk.cef.network
import akka.util.ByteString
import io.iohk.cef.utils.HexStringCodec._
import io.iohk.cef.codecs.nio._

/**
  * A node id represents an addressable identity on a cef network.
  */
case class NodeId private[network] (id: ByteString) {
  require(id.size == NodeId.nodeIdBytes, s"A node ID has to be of is ${NodeId.nodeIdBytes} bytes")
  override def toString: String = toHexString(id)
}

object NodeId {
  val nodeIdBytes: Int = 2
  val nodeIdBits: Int = nodeIdBytes * 8

  def apply(idBytes: Seq[Byte]): NodeId =
    NodeId(ByteString(idBytes.toArray))

  def apply(idHex: String): NodeId =
    NodeId(fromHexString(idHex))

  implicit val NodeIdEncDec: NioEncDec[NodeId] = {
    import io.iohk.cef.codecs.nio.auto._
    val e: NioEncoder[NodeId] = genericEncoder
    val d: NioDecoder[NodeId] = genericDecoder
    NioEncDec(e, d)
  }
}
