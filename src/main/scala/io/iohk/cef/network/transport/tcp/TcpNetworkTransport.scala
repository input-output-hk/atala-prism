package io.iohk.cef.network.transport.tcp

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.transport.NetworkTransport

class TcpNetworkTransport[Message](messageHandler: (InetSocketAddress, Message) => Unit,
                                   nettyTransport: NettyTransport)
                                  (implicit encoder: NioEncoder[Message], decoder: NioDecoder[Message])
  extends NetworkTransport[InetSocketAddress, Message](messageHandler) {

  nettyTransport.withMessageApplication(decoder, messageHandler)

  override def sendMessage(address: InetSocketAddress, message: Message): Unit =
    nettyTransport.sendMessage(address, encode(message))

  private def encode(message: Message): ByteBuffer =
    encoder.encode(message)
}
