package io.iohk.cef.network.transport
import java.net.InetSocketAddress

import io.iohk.cef.network.PeerInfo
import io.iohk.cef.network.encoding.nio.NioCodecs.NioStreamCodec
import io.iohk.cef.network.transport.tcp.{NettyTransport, TcpNetworkTransport}

object Transports {
  def usesTcp(peerInfo: PeerInfo): Boolean =
    peerInfo.configuration.tcpTransportConfiguration.isDefined
}

class Transports(peerInfo: PeerInfo) {

  private var nettyTransportRef: Option[NettyTransport] = None

  def netty(): Option[NettyTransport] = this.synchronized {
    peerInfo.configuration.tcpTransportConfiguration.map(tcpConfiguration => {
      nettyTransportRef match {
        case Some(nettyTransport) =>
          nettyTransport
        case None =>
          val nettyTransport = new NettyTransport(tcpConfiguration.bindAddress)
          nettyTransportRef = Some(nettyTransport)
          nettyTransport
      }
    })
  }

  def tcp[T](messageHandler: (InetSocketAddress, T) => Unit)(
      implicit codec: NioStreamCodec[T]): Option[TcpNetworkTransport[T]] =
    netty().map(nettyTransport => new TcpNetworkTransport[T](messageHandler, codec, nettyTransport))
}
