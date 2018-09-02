package io.iohk.cef.network.discovery

import akka.util.ByteString
import io.iohk.cef.network.encoding.rlp._
import io.iohk.cef.network.{Capabilities, NodeInfo}

sealed trait DiscoveryWireMessage {

  def messageType: Byte
}

case class Ping(protocolVersion: Int, node: NodeInfo, timestamp: Long, nonce: ByteString) extends DiscoveryWireMessage {
  override def messageType: Byte = Ping.messageType
}

object Ping {

  val messageType: Byte = 0x01

  implicit def pingRLPEncDec(
      implicit
      byteEncDec: RLPEncDec[Byte],
      intEncDec: RLPEncDec[Int],
      nodeEncDec: RLPEncDec[NodeInfo],
      longEncDec: RLPEncDec[Long],
      byteStrEncDec: RLPEncDec[ByteString]) = new RLPEncDec[Ping] {

    override def encode(obj: Ping): RLPEncodeable =
      RLPList(
        byteEncDec.encode(obj.messageType),
        intEncDec.encode(obj.protocolVersion),
        nodeEncDec.encode(obj.node),
        longEncDec.encode(obj.timestamp),
        byteStrEncDec.encode(obj.nonce)
      )

    override def decode(rlp: RLPEncodeable): Ping = rlp match {
      case RLPList(messageType, protocolVersion, from, timestamp, nonce)
          if byteEncDec.decode(messageType) == Ping.messageType =>
        Ping(
          intEncDec.decode(protocolVersion),
          nodeEncDec.decode(from),
          longEncDec.decode(timestamp),
          byteStrEncDec.decode(nonce))
      case _ => throw new RLPException("src is not a valid Ping message")
    }
  }
}

case class Pong(node: NodeInfo, token: ByteString, timestamp: Long) extends DiscoveryWireMessage {
  override def messageType: Byte = Pong.messageType

}

object Pong {

  val messageType: Byte = 0x02

  implicit def pongRLPEncDec(
      implicit
      byteEncDec: RLPEncDec[Byte],
      nodeEncDec: RLPEncDec[NodeInfo],
      byteStrEncDec: RLPEncDec[ByteString],
      longEncDec: RLPEncDec[Long]) = new RLPEncDec[Pong] {

    override def encode(obj: Pong): RLPEncodeable =
      RLPList(
        byteEncDec.encode(obj.messageType),
        nodeEncDec.encode(obj.node),
        byteStrEncDec.encode(obj.token),
        longEncDec.encode(obj.timestamp))

    override def decode(rlp: RLPEncodeable): Pong = rlp match {
      case RLPList(messageType, node, token, timestamp) if byteEncDec.decode(messageType) == Pong.messageType =>
        Pong(nodeEncDec.decode(node), byteStrEncDec.decode(token), longEncDec.decode(timestamp))
      case _ => throw new RLPException("src is not a valid Pong message")
    }
  }
}

case class Seek(capabilities: Capabilities, maxResults: Int, timestamp: Long, nonce: ByteString)
    extends DiscoveryWireMessage {
  override def messageType: Byte = Seek.messageType
}

object Seek {

  val messageType: Byte = 0x03

  implicit def seekRLPEncDec(
      implicit
      byteEncDec: RLPEncDec[Byte],
      capabilitiesEncDec: RLPEncDec[Capabilities],
      intEncDec: RLPEncDec[Int],
      longEncDec: RLPEncDec[Long],
      byteStringEncDec: RLPEncDec[ByteString]) = new RLPEncDec[Seek] {

    override def encode(obj: Seek): RLPEncodeable =
      RLPList(
        byteEncDec.encode(obj.messageType),
        capabilitiesEncDec.encode(obj.capabilities),
        intEncDec.encode(obj.maxResults),
        longEncDec.encode(obj.timestamp),
        byteStringEncDec.encode(obj.nonce)
      )

    override def decode(rlp: RLPEncodeable): Seek = rlp match {
      case RLPList(messageType, capabilities, maxResults, timestamp, nonce)
          if byteEncDec.decode(messageType) == Seek.messageType =>
        Seek(
          capabilitiesEncDec.decode(capabilities),
          intEncDec.decode(maxResults),
          longEncDec.decode(timestamp),
          byteStringEncDec.decode(nonce))
      case _ => throw new RLPException("src is not a valid Seek message")
    }
  }
}

case class Neighbors(
    capabilities: Capabilities,
    token: ByteString,
    neighborsWithCapabilities: Int,
    neighbors: Seq[NodeInfo],
    timestamp: Long)
    extends DiscoveryWireMessage {
  override def messageType: Byte = Neighbors.messageType
}

object Neighbors {

  val messageType: Byte = 0x04

  implicit def neighborsRLPEncDec(
      implicit
      byteEncDec: RLPEncDec[Byte],
      byteStringEncDec: RLPEncDec[ByteString],
      capabilitiesEncDec: RLPEncDec[Capabilities],
      intStrEncDec: RLPEncDec[Int],
      seqNodeEncDec: RLPEncDec[Seq[NodeInfo]],
      longEncDec: RLPEncDec[Long]) = new RLPEncDec[Neighbors] {

    override def encode(obj: Neighbors): RLPEncodeable =
      RLPList(
        byteEncDec.encode(obj.messageType),
        byteStringEncDec.encode(obj.token),
        capabilitiesEncDec.encode(obj.capabilities),
        intStrEncDec.encode(obj.neighborsWithCapabilities),
        seqNodeEncDec.encode(obj.neighbors),
        longEncDec.encode(obj.timestamp)
      )

    override def decode(rlp: RLPEncodeable): Neighbors = rlp match {
      case RLPList(messageType, token, capabilities, neighborsWithCap, neighbors, timestamp)
          if byteEncDec.decode(messageType) == Neighbors.messageType =>
        Neighbors(
          capabilitiesEncDec.decode(capabilities),
          byteStringEncDec.decode(token),
          intStrEncDec.decode(neighborsWithCap),
          seqNodeEncDec.decode(neighbors),
          longEncDec.decode(timestamp)
        )
      case _ => throw new RLPException("src is not a valid Neighbors message")
    }
  }
}

object DiscoveryWireMessage {

  val ProtocolVersion = 1
  implicit def RLPEncDec(
      implicit
      byteEncDec: RLPEncDec[Byte],
      pingEncDec: RLPEncDec[Ping],
      pongEncDec: RLPEncDec[Pong],
      seekEncDec: RLPEncDec[Seek],
      neighborsEncDec: RLPEncDec[Neighbors]) =
    new RLPEncDec[DiscoveryWireMessage] {

      override def encode(obj: DiscoveryWireMessage): RLPEncodeable =
        obj match {
          case ping: Ping => pingEncDec.encode(ping)
          case pong: Pong => pongEncDec.encode(pong)
          case seek: Seek => seekEncDec.encode(seek)
          case neighbors: Neighbors => neighborsEncDec.encode(neighbors)
        }

      override def decode(rlp: RLPEncodeable): DiscoveryWireMessage = rlp match {
        case list: RLPList if list.items.size > 1 =>
          val messageType = byteEncDec.decode(list.items.head)
          if (messageType == Ping.messageType)
            pingEncDec.decode(rlp)
          else if (messageType == Pong.messageType)
            pongEncDec.decode(rlp)
          else if (messageType == Seek.messageType)
            seekEncDec.decode(rlp)
          else if (messageType == Neighbors.messageType)
            neighborsEncDec.decode(rlp)
          else throw new RLPException("src is not a valid DiscoveryMessage")
        case _ => throw new RLPException("src is not a valid DiscoveryMessage")
      }
    }
}
