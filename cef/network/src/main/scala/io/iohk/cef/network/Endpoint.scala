package io.iohk.cef.network

import java.net.{InetSocketAddress, _}

import io.iohk.cef.encoding.rlp._

case class Endpoint(address: InetAddress, udpPort: Int, tcpPort: Int) {

  lazy val udpSocketAddress = new InetSocketAddress(address, udpPort)
  lazy val tcpSocketAddress = new InetSocketAddress(address, tcpPort)

  lazy val endpoint = Endpoint(address, udpPort, tcpPort)

  def equalIpAddress(that: Endpoint) = {
    this.address.getAddress.sameElements(that.address.getAddress)
  }

  def equalUdpAddress(that: Endpoint): Boolean = {
    equalUdpAddress(that.udpSocketAddress)
  }

  def equalTcpAddress(that: Endpoint): Boolean = {
    equalTcpAddress(that.tcpSocketAddress)
  }

  def equalIpAddress(that: InetAddress): Boolean = {
    this.address.getAddress.sameElements(that.getAddress)
  }

  def equalUdpAddress(that: InetSocketAddress): Boolean = {
    equalIpAddress(that.getAddress) && udpPort == that.getPort
  }

  def equalTcpAddress(that: InetSocketAddress): Boolean = {
    equalIpAddress(that.getAddress) && tcpPort == that.getPort
  }

}

object Endpoint {

  implicit def nodeAddressRLPEncDec(implicit
                                    addrEncDec: RLPEncDec[InetAddress],
                                    intEncDec: RLPEncDec[Int]) = new RLPEncDec[Endpoint] {
    override def encode(obj: Endpoint): RLPEncodeable =
      RLPList(addrEncDec.encode(obj.address), intEncDec.encode(obj.udpPort), intEncDec.encode(obj.tcpPort))

    override def decode(rlp: RLPEncodeable): Endpoint = rlp match {
      case RLPList(addr, udpPort, tcpPort) =>
        Endpoint(addrEncDec.decode(addr), intEncDec.decode(udpPort), intEncDec.decode(tcpPort))
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

  def fromUdpAddress(udpAddress: InetSocketAddress, tcpPort: Int): Endpoint =
    Endpoint(udpAddress.getAddress, udpAddress.getPort, tcpPort)
}
