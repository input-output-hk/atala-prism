package io.iohk.cef.network.transport

import java.net.InetSocketAddress

import io.iohk.cef.network.PeerInfo
import io.iohk.cef.codecs.nio._
import io.iohk.cef.network.transport.tcp.{NettyTransport, TcpNetworkTransport}

object Transports {
  def usesTcp(peerInfo: PeerInfo): Boolean =
    peerInfo.configuration.tcpTransportConfiguration.isDefined
}

/**
  * Encapsulates the networking resources held by the node (tcp ports, etc).
  * You only want one of these objects in most application configurations.
  * @param peerInfo configuration data for the node.
  */
class Transports(val peerInfo: PeerInfo) {

  private var nettyTransportRef: Option[NettyTransport] = None

  def netty(): Option[NettyTransport] = this.synchronized { // AtomicRef does not work for side-effecting fns
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

  def tcp[T](implicit codec: NioCodec[T]): Option[NetworkTransport[InetSocketAddress, T]] =
    netty().map(nettyTransport => new TcpNetworkTransport[T](nettyTransport)(codec.encoder, codec.decoder))

  def shutdown(): Unit = {
    nettyTransportRef.foreach(_.shutdown())
  }
}
