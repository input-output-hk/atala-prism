package io.iohk.cef.discovery

import akka.util.ByteString
import io.iohk.cef.encoding.rlp._
import io.iohk.cef.network.{Capabilities, Endpoint, Node}

sealed trait DiscoveryMessage {

  def messageType: Byte
}

case class Ping(protocolVersion: Int, replyTo: Endpoint, timestamp: Long, nonce: Array[Byte]) extends DiscoveryMessage {
  override def messageType: Byte = Ping.messageType
}

object Ping {

  val messageType: Byte = 0x01

  implicit def pingRLPEncDec(implicit
                             byteEncDec: RLPEncDec[Byte],
                             intEncDec: RLPEncDec[Int],
                             endpointEncDec: RLPEncDec[Endpoint],
                             longEncDec: RLPEncDec[Long],
                             arrayByteEncDec: RLPEncDec[Array[Byte]]) = new RLPEncDec[Ping] {

    override def encode(obj: Ping): RLPEncodeable =
      RLPList(byteEncDec.encode(obj.messageType),
        intEncDec.encode(obj.protocolVersion),
        endpointEncDec.encode(obj.replyTo),
        longEncDec.encode(obj.timestamp),
        arrayByteEncDec.encode(obj.nonce))

    override def decode(rlp: RLPEncodeable): Ping = rlp match {
      case RLPList(messageType, protocolVersion, from, timestamp, nonce) if byteEncDec.decode(messageType) == Ping.messageType =>
        Ping(intEncDec.decode(protocolVersion),
          endpointEncDec.decode(from),
          longEncDec.decode(timestamp),
          arrayByteEncDec.decode(nonce))
      case _ => throw new RLPException("src is not a valid Ping message")
    }
  }
}

case class Pong(capabilities: Capabilities, token: ByteString, timestamp: Long) extends DiscoveryMessage {
  override def messageType: Byte = Pong.messageType

}

object Pong {

  val messageType: Byte = 0x02

  implicit def pongRLPEncDec(implicit
                             byteEncDec: RLPEncDec[Byte],
                             capabilitiesEncDec: RLPEncDec[Capabilities],
                             byteStrEncDec: RLPEncDec[ByteString],
                             longEncDec: RLPEncDec[Long]) = new RLPEncDec[Pong] {

    override def encode(obj: Pong): RLPEncodeable =
      RLPList(byteEncDec.encode(obj.messageType),
        capabilitiesEncDec.encode(obj.capabilities),
        byteStrEncDec.encode(obj.token),
        longEncDec.encode(obj.timestamp))

    override def decode(rlp: RLPEncodeable): Pong = rlp match {
      case RLPList(messageType, capabilities, token, timestamp) if byteEncDec.decode(messageType) == Pong.messageType =>
        Pong(capabilitiesEncDec.decode(capabilities), byteStrEncDec.decode(token), longEncDec.decode(timestamp))
      case _ => throw new RLPException("src is not a valid Pong message")
    }
  }
}

case class Seek(capabilities: Capabilities, maxResults: Int, timestamp: Long) extends DiscoveryMessage {
  override def messageType: Byte = Seek.messageType
}

object Seek {

  val messageType: Byte = 0x03

  implicit def seekRLPEncDec(implicit
                             byteEncDec: RLPEncDec[Byte],
                             capabilitiesEncDec: RLPEncDec[Capabilities],
                             intEncDec: RLPEncDec[Int],
                             longEncDec: RLPEncDec[Long]) = new RLPEncDec[Seek] {

    override def encode(obj: Seek): RLPEncodeable =
      RLPList(byteEncDec.encode(obj.messageType),
        capabilitiesEncDec.encode(obj.capabilities),
        intEncDec.encode(obj.maxResults),
        longEncDec.encode(obj.timestamp))

    override def decode(rlp: RLPEncodeable): Seek = rlp match {
      case RLPList(messageType, capabilities, maxResults, timestamp) if byteEncDec.decode(messageType) == Seek.messageType =>
        Seek(capabilitiesEncDec.decode(capabilities), intEncDec.decode(maxResults), longEncDec.decode(timestamp))
      case _ => throw new RLPException("src is not a valid Seek message")
    }
  }
}

case class Neighbors(capabilities: Capabilities,
                     token: ByteString,
                     neighborsWithCapabilities: Int,
                     neighbors: Seq[Node],
                     timestamp: Long) extends DiscoveryMessage {
  override def messageType: Byte = Neighbors.messageType
}

object Neighbors {

  val messageType: Byte = 0x04

  implicit def neighborsRLPEncDec(implicit
                                  byteEncDec: RLPEncDec[Byte],
                                  byteStringEncDec: RLPEncDec[ByteString],
                                  capabilitiesEncDec: RLPEncDec[Capabilities],
                                  intStrEncDec: RLPEncDec[Int],
                                  seqNodeEncDec: RLPEncDec[Seq[Node]],
                                  longEncDec: RLPEncDec[Long]) = new RLPEncDec[Neighbors] {

    override def encode(obj: Neighbors): RLPEncodeable =
      RLPList(byteEncDec.encode(obj.messageType),
        byteStringEncDec.encode(obj.token),
        capabilitiesEncDec.encode(obj.capabilities),
        intStrEncDec.encode(obj.neighborsWithCapabilities),
        seqNodeEncDec.encode(obj.neighbors),
        longEncDec.encode(obj.timestamp))

    override def decode(rlp: RLPEncodeable): Neighbors = rlp match {
      case RLPList(messageType, token, capabilities, neighborsWithCap, neighbors, timestamp)
          if byteEncDec.decode(messageType) == Neighbors.messageType =>
        Neighbors(capabilitiesEncDec.decode(capabilities),
          byteStringEncDec.decode(token),
          intStrEncDec.decode(neighborsWithCap),
          seqNodeEncDec.decode(neighbors),
          longEncDec.decode(timestamp))
      case _ => throw new RLPException("src is not a valid Neighbors message")
    }
  }
}


object DiscoveryMessage {

  val ProtocolVersion = 1
  implicit def RLPEncDec(implicit
                         byteEncDec: RLPEncDec[Byte],
                         pingEncDec: RLPEncDec[Ping],
                         pongEncDec: RLPEncDec[Pong],
                         seekEncDec: RLPEncDec[Seek],
                         neighborsEncDec: RLPEncDec[Neighbors]) =
  new RLPEncDec[DiscoveryMessage] {

    override def encode(obj: DiscoveryMessage): RLPEncodeable =
      obj match {
        case ping: Ping => pingEncDec.encode(ping)
        case pong: Pong => pongEncDec.encode(pong)
        case seek: Seek => seekEncDec.encode(seek)
        case neighbors: Neighbors => neighborsEncDec.encode(neighbors)
      }

    override def decode(rlp: RLPEncodeable): DiscoveryMessage = rlp match {
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
