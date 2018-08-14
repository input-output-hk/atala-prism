package io.iohk.cef.network.transport.tcp

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import io.iohk.cef.network.encoding.nio.NioCodecs.NioStreamCodec
import io.iohk.cef.network.transport.NetworkTransport

class TcpNetworkTransport[Message](messageHandler: (InetSocketAddress, Message) => Unit,
                                   codec: NioStreamCodec[Message],
                                   nettyTransport: NettyTransport)
  extends NetworkTransport[InetSocketAddress, Message](messageHandler) {

  nettyTransport.withMessageHandler(nettyMessageHandler)

  override def sendMessage(address: InetSocketAddress, message: Message): Unit =
    nettyTransport.sendMessage(address, encode(message))


  private def encode(message: Message): ByteBuffer =
    codec.encoder.encode(message)

  // TODO messageHandlers for each message type will need to peek at the buffer
  // to determine whether to call decode.
  // Or the stream decoders will need to read type info from the start of the buff
  // and skip decoding if it is not their type.
  private def nettyMessageHandler(address: InetSocketAddress, byteBuffer: ByteBuffer): Unit =
    decode(byteBuffer).foreach(message => messageHandler(address, message))

  private def decode(byteBuf: ByteBuffer): Seq[Message] = {
    codec.decoder.decodeStream(byteBuf)
  }
}
