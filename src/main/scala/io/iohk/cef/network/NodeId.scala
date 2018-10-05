package io.iohk.cef.network
import akka.util.ByteString
import io.iohk.cef.utils.HexStringCodec._

/**
  * A node id represents an addressable identity on a cef network.
  */
case class NodeId private[network] (id: ByteString) {
  override def toString: String = toHexString(id)
}

object NodeId {
  val nodeIdBytes: Int = 2
  val nodeIdBits: Int = 2 * 8

  def apply(idBytes: Seq[Byte]): NodeId = {
    if (idBytes.length != 2)
      throw new IllegalArgumentException("A node ID is 2 bytes")
    else
      NodeId(ByteString(idBytes.toArray))
  }

  def apply(idHex: String): NodeId =
    NodeId(fromHexString(idHex))
}
