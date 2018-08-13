package io.iohk.cef.network.transport.tcp

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import io.iohk.cef.network.encoding.StreamCodec
import io.iohk.cef.network.transport.NetworkTransport

class TcpNetworkTransport[Message](messageHandler: (InetSocketAddress, Message) => Unit,
                                   codec: StreamCodec[Message, ByteBuffer],
                                   nettyTransport: NettyTransport)
  extends NetworkTransport[InetSocketAddress, Message](messageHandler) {

  nettyTransport.withMessageHandler(nettyMessageHandler)

  override def sendMessage(address: InetSocketAddress, message: Message): Unit =
    nettyTransport.sendMessage(address, encode(message))


  private def encode(message: Message): ByteBuffer =
    codec.encoder.encode(message)

  private def nettyMessageHandler(address: InetSocketAddress, byteBuffer: ByteBuffer): Unit =
    decode(byteBuffer).foreach(message => messageHandler(address, message))

  private def decode(byteBuf: ByteBuffer): Seq[Message] = {
    codec.decoder.decodeStream(byteBuf)
  }
}
