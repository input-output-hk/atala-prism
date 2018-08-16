package io.iohk.cef.network
import akka.util.ByteString
import org.bouncycastle.util.encoders.Hex

/**
  * A node id represents an addressable identity on cef network.
  */
case class NodeId private[network](id: ByteString)

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
    NodeId(Hex.decode(idHex))
}
