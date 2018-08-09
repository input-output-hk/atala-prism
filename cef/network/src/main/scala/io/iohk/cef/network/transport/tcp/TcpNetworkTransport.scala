package io.iohk.cef.network.transport.tcp

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import io.iohk.cef.network.encoding.StreamCodec
import io.iohk.cef.network.transport.NetworkTransport

class TcpNetworkTransport[Message](messageHandler: (InetSocketAddress, Message) => Unit,
                                   codec: StreamCodec[Message, ByteBuffer],
                                   configuration: TcpTransportConfiguration)
  extends NetworkTransport[InetSocketAddress, Message](messageHandler) {

  /**
    * The primary purpose of this method is to decouple the creation
    * and the 'turning on' of a transport.
    *
    * This method should make the transport ready
    * for sending and receiving messages. This might mean, for example,
    * binding to an address.
    *
    * This method should run synchronously then return the Transport in its new state.
    *
    * throws TransportInitializationException if initialization fails.
    */
  override def start(): TcpNetworkTransport[Message] = {

    val server = new NettyTransport(configuration.bindAddress, codec, messageHandler).start()

    new TcpNetworkTransport(messageHandler, codec, configuration) {
      override def start(): TcpNetworkTransport[Message] = this

      override def sendMessage(address: InetSocketAddress, message: Message): Unit =
        server.sendMessage(address, message)
    }
  }

  override def sendMessage(address: InetSocketAddress, message: Message): Unit =
    throw new UnsupportedOperationException("The transport has not been started.")
}
