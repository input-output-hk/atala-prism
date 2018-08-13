package io.iohk.cef.network.transport.tcp
import java.net.InetSocketAddress

case class TcpTransportConfiguration(bindAddress: InetSocketAddress, natAddress: InetSocketAddress)

object TcpTransportConfiguration {
  def apply(bindAddress: InetSocketAddress): TcpTransportConfiguration =
    TcpTransportConfiguration(bindAddress, bindAddress)
}
