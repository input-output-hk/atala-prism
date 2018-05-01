package io.iohk.cef.network

import java.net.{InetAddress, InetSocketAddress}

case class Endpoint(address: InetAddress, udpPort: Int, tcpPort: Int) {

  def toUdpAddress: InetSocketAddress = {
    new InetSocketAddress(address, udpPort)
  }
}


object Endpoint {

  def fromUdpAddress(udpAddress: InetSocketAddress, tcpPort: Int): Endpoint =
    Endpoint(udpAddress.getAddress, udpAddress.getPort, tcpPort)

}