package io.iohk.cef.network

import java.net.{InetSocketAddress, _}

import akka.util.ByteString
import io.iohk.cef.encoding.rlp._
import org.bouncycastle.util.encoders.Hex

import scala.util.{Failure, Success, Try}

case class NodeAddress(id: ByteString, addr: InetAddress, tcpPort: Int, udpPort: Int) {

  lazy val udpSocketAddress = new InetSocketAddress(addr, udpPort)
  lazy val tcpSocketAddress = new InetSocketAddress(addr, tcpPort)

  lazy val endpoint = Endpoint(addr, udpPort, tcpPort)

  def toUri: URI = {
    val host = NodeAddress.getHostName(addr)
    new URI(s"enode://${id.utf8String}@$host:$tcpPort?discport=$udpPort")
  }
}

object NodeAddress {

  implicit def nodeAddressRLPEncDec(implicit
                                   byteStrEncDec: RLPEncDec[ByteString],
                                    addrEncDec: RLPEncDec[InetAddress],
                                    intEncDec: RLPEncDec[Int]) = new RLPEncDec[NodeAddress] {
    override def encode(obj: NodeAddress): RLPEncodeable =
      RLPList(byteStrEncDec.encode(obj.id), addrEncDec.encode(obj.addr), intEncDec.encode(obj.tcpPort), intEncDec.encode(obj.udpPort))

    override def decode(rlp: RLPEncodeable): NodeAddress = rlp match {
      case RLPList(id, addr, tcpPort, udpPort) =>
        NodeAddress(byteStrEncDec.decode(id), addrEncDec.decode(addr), intEncDec.decode(tcpPort), intEncDec.decode(udpPort))
      case _ => throw new RLPException("src is not a valid NodeAddress")
    }
  }

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

  def fromUri(uri: URI): NodeAddress = {
    val nodeId = ByteString(Hex.decode(uri.getUserInfo))
    val address = InetAddress.getByName(uri.getHost)
    val tcpPort = uri.getPort
    //Where should we store the capabilities?
    NodeAddress(nodeId, address, tcpPort, getUdpPort(uri, tcpPort))
  }
}
