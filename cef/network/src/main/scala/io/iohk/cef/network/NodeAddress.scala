package io.iohk.cef.network

import java.net.{InetSocketAddress, _}

import io.iohk.cef.encoding.rlp._

case class NodeAddress(addr: InetAddress, tcpPort: Int, udpPort: Int) {

  lazy val udpSocketAddress = new InetSocketAddress(addr, udpPort)
  lazy val tcpSocketAddress = new InetSocketAddress(addr, tcpPort)

  lazy val endpoint = Endpoint(addr, udpPort, tcpPort)

  def equalIpAddress(that: NodeAddress) = {
    this.addr.getAddress.sameElements(that.addr.getAddress)
  }

  def equalUdpAddress(that: NodeAddress) = {
    equalIpAddress(that) && udpPort == that.udpPort
  }

  def equalTcpAddress(that: NodeAddress) = {
    equalIpAddress(that) && tcpPort == that.tcpPort
  }

  def equalIpAddress(that: InetAddress) = {
    this.addr.getAddress.sameElements(that.getAddress)
  }

  def equalUdpAddress(that: InetSocketAddress) = {
    equalIpAddress(that.getAddress) && udpPort == that.getPort
  }

  def equalTcpAddress(that: InetSocketAddress) = {
    equalIpAddress(that.getAddress) && tcpPort == that.getPort
  }
}

object NodeAddress {

  implicit def nodeAddressRLPEncDec(implicit
                                    addrEncDec: RLPEncDec[InetAddress],
                                    intEncDec: RLPEncDec[Int]) = new RLPEncDec[NodeAddress] {
    override def encode(obj: NodeAddress): RLPEncodeable =
      RLPList(addrEncDec.encode(obj.addr), intEncDec.encode(obj.tcpPort), intEncDec.encode(obj.udpPort))

    override def decode(rlp: RLPEncodeable): NodeAddress = rlp match {
      case RLPList(addr, tcpPort, udpPort) =>
        NodeAddress(addrEncDec.decode(addr), intEncDec.decode(tcpPort), intEncDec.decode(udpPort))
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
}
