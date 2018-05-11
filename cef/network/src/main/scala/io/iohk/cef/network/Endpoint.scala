package io.iohk.cef.network

import java.net.{InetSocketAddress, _}

import io.iohk.cef.encoding.rlp._

case class NodeAddress(address: InetAddress, udpPort: Int, tcpPort: Int) {

  lazy val udpSocketAddress = new InetSocketAddress(address, udpPort)
  lazy val tcpSocketAddress = new InetSocketAddress(address, tcpPort)

  lazy val endpoint = NodeAddress(address, udpPort, tcpPort)

  def equalIpAddress(that: NodeAddress) = {
    this.address.getAddress.sameElements(that.address.getAddress)
  }

  def equalUdpAddress(that: NodeAddress) = {
    equalIpAddress(that) && udpPort == that.udpPort
  }

  def equalTcpAddress(that: NodeAddress) = {
    equalIpAddress(that) && tcpPort == that.tcpPort
  }

  def equalIpAddress(that: InetAddress) = {
    this.address.getAddress.sameElements(that.getAddress)
  }

  def equalUdpAddress(that: InetSocketAddress) = {
    equalIpAddress(that.getAddress) && udpPort == that.getPort
  }

  def equalTcpAddress(that: InetSocketAddress) = {
    equalIpAddress(that.getAddress) && tcpPort == that.getPort
  }
  def toUdpAddress: InetSocketAddress = {
    new InetSocketAddress(address, udpPort)
  }
}

object NodeAddress {

  implicit def nodeAddressRLPEncDec(implicit
                                    addrEncDec: RLPEncDec[InetAddress],
                                    intEncDec: RLPEncDec[Int]) = new RLPEncDec[NodeAddress] {
    override def encode(obj: NodeAddress): RLPEncodeable =
      RLPList(addrEncDec.encode(obj.address), intEncDec.encode(obj.udpPort), intEncDec.encode(obj.tcpPort))

    override def decode(rlp: RLPEncodeable): NodeAddress = rlp match {
      case RLPList(addr, udpPort, tcpPort) =>
        NodeAddress(addrEncDec.decode(addr), intEncDec.decode(udpPort), intEncDec.decode(tcpPort))
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

  def fromUdpAddress(udpAddress: InetSocketAddress, tcpPort: Int): NodeAddress =
    NodeAddress(udpAddress.getAddress, udpAddress.getPort, tcpPort)
}
