package io.iohk.cef.discovery

import akka.util.ByteString
import io.iohk.cef.network.{Capabilities, Endpoint, Node}

sealed trait DiscoveryMessage {

  def messageType: Byte
}

object DiscoveryMessage {

  val ProtocolVersion = 1

  case class Ping(protocolVersion: Int, from: Endpoint, timestamp: Long) extends DiscoveryMessage {
    override def messageType: Byte = 0x01
  }

  case class Pong(capabilities: Capabilities, token: ByteString, timestamp: Long) extends DiscoveryMessage {
    override def messageType: Byte = 0x02

  }

  case class Seek(capabilities: Capabilities, maxResults: Int) extends DiscoveryMessage {
    override def messageType: Byte = 0x03
  }

  case class Neighbors(capabilities: Capabilities,
                       neighborsWithCapabilities: Int,
                       neighbors: Seq[Node]) extends DiscoveryMessage {
    override def messageType: Byte = 0x04
  }

}
