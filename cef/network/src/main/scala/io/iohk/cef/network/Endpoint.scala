package io.iohk.cef.network

import java.net.{InetAddress, InetSocketAddress}

import io.iohk.cef.encoding.rlp._

case class Endpoint(address: InetAddress, udpPort: Int, tcpPort: Int) {

  def toUdpAddress: InetSocketAddress = {
    new InetSocketAddress(address, udpPort)
  }
}


object Endpoint {

  def fromUdpAddress(udpAddress: InetSocketAddress, tcpPort: Int): Endpoint =
    Endpoint(udpAddress.getAddress, udpAddress.getPort, tcpPort)

  implicit def endpointRLPEncDec(implicit
                                intEncDec: RLPEncDec[Int],
                                 addrEncDec: RLPEncDec[InetAddress]) =
  new RLPEncDec[Endpoint] {

    override def encode(obj: Endpoint): RLPEncodeable =
      RLPList(addrEncDec.encode(obj.address), intEncDec.encode(obj.udpPort), intEncDec.encode(obj.tcpPort))

    override def decode(rlp: RLPEncodeable): Endpoint = rlp match {
      case RLPList(address,udpPort,tcpPort) =>
        Endpoint(addrEncDec.decode(address), intEncDec.decode(udpPort), intEncDec.decode(tcpPort))
    }
  }

}