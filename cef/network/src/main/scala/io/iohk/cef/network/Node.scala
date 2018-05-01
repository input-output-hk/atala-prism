package io.iohk.cef.network

import java.net.{InetSocketAddress, _}

import akka.util.ByteString
import org.bouncycastle.util.encoders.Hex

import scala.util.{Failure, Success, Try}

case class Node(id: ByteString, capabilities: Capabilities, addr: InetAddress, tcpPort: Int, udpPort: Int) {

  lazy val udpSocketAddress = new InetSocketAddress(addr, udpPort)
  lazy val tcpSocketAddress = new InetSocketAddress(addr, tcpPort)

  lazy val endpoint = Endpoint(addr, udpPort, tcpPort)

  def toUri: URI = {
    val host = Node.getHostName(addr)
    new URI(s"enode://${Hex.toHexString(id.toArray[Byte])}@$host:$tcpPort?discport=$udpPort")
  }
}

object Node {

  /**
    * Given an address, returns the corresponding host name for the URI.
    * All IPv6 addresses are enclosed in square brackets.
    *
    * @param address, whose host name will be obtained
    * @return host name associated with the address
    */
  def getHostName(address: InetAddress): String = {
    val hostName = address.getHostAddress
    address match {
      case _: Inet6Address => s"[$hostName]"
      case _ => hostName
    }
  }

  // If there is no udp port specified or it is malformed use tcp as default
  private def getUdpPort(uri: URI, default: Int): Int = {
    Option(uri.getQuery).fold(default) { query =>
      Try {
        val params = query.split("=")
        if (params(0) == "discport")
          params(1).toInt
        else
          default
      } match {
        case Success(udpPort) => udpPort
        case Failure(_) => default
      }
    }
  }

  def fromUri(uri: URI): Node = {
    val nodeId = ByteString(Hex.decode(uri.getUserInfo))
    val address = InetAddress.getByName(uri.getHost)
    val tcpPort = uri.getPort
    Node(nodeId, address, tcpPort, getUdpPort(uri, tcpPort))
  }
}
