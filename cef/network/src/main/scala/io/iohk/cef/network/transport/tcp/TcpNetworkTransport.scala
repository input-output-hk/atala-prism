package io.iohk.cef.network.transport.tcp

import java.net.InetSocketAddress

import io.iohk.cef.network.encoding.{Encoder, StreamDecoder}
import io.iohk.cef.network.transport.NetworkTransport
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.ReferenceCountUtil

case class TcpNetworkConfiguration(bindAddress: InetSocketAddress)

private[tcp] class NettyServer[Message](address: InetSocketAddress,
                                        encoder: Encoder[Message, ByteBuf],
                                        decoder: StreamDecoder[ByteBuf, Message],
                                        messageHandler: (InetSocketAddress, Message) => Unit) {

  import io.netty.bootstrap.ServerBootstrap
  import io.netty.buffer.ByteBuf
  import io.netty.channel._
  import io.netty.channel.nio.NioEventLoopGroup
  import io.netty.channel.socket.SocketChannel
  import io.netty.channel.socket.nio.NioServerSocketChannel

  class NettyDecoder extends ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
      decoder.decodeStream(msg.asInstanceOf[ByteBuf]).foreach(message => messageHandler(ctx.channel().remoteAddress().asInstanceOf[InetSocketAddress], message))
    }
  }

  class NettyEncoder extends ChannelOutboundHandlerAdapter {
    override def write(ctx: ChannelHandlerContext, msg: Object, promise: ChannelPromise): Unit = {
      val buf = encoder.encode(msg.asInstanceOf[Message])
      try {
        try {
          ctx.write(buf, promise)
        } finally {
          buf.release()
        }
      } finally {
        ReferenceCountUtil.release(msg)
      }
    }
  }

  def start(): NettyServer[Message] = {
    val bossGroup = new NioEventLoopGroup
    val workerGroup = new NioEventLoopGroup
    val b = new ServerBootstrap
    b.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[SocketChannel]() {
        override def initChannel(ch: SocketChannel): Unit = {
          ch.pipeline().addLast(new NettyDecoder())
        }
      })
      .option[Integer](ChannelOption.SO_BACKLOG, 128)
      .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)

    b.bind(address).await()
    this
  }

  def sendMessage(address: InetSocketAddress, message: Message): Unit = {

    val workerGroup = new NioEventLoopGroup()

    val activationAdapter = new ChannelInboundHandlerAdapter() {
      override def channelActive(ctx: ChannelHandlerContext): Unit = {
        val buf: ByteBuf = encoder.encode(message)
        try {
          ctx.writeAndFlush(buf)
        } finally {
          workerGroup.shutdownGracefully()
        }
      }
    }

    new Bootstrap()
      .group(workerGroup)
      .channel(classOf[NioSocketChannel])
      .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
      .handler(new ChannelInitializer[SocketChannel]() {
        def initChannel(ch: SocketChannel): Unit = {
          ch.pipeline().addLast(new NettyDecoder(), activationAdapter)
        }
      }).connect(address)
  }
}

class TcpNetworkTransport[Message](messageHandler: (InetSocketAddress, Message) => Unit,
                                   encoder: Encoder[Message, ByteBuf],
                                   decoder: StreamDecoder[ByteBuf, Message],
                                   configuration: TcpNetworkConfiguration)
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

    val server = new NettyServer(configuration.bindAddress, encoder, decoder, messageHandler).start()

    new TcpNetworkTransport(messageHandler, encoder, decoder, configuration) {
      override def start(): TcpNetworkTransport[Message] = this

      override def sendMessage(address: InetSocketAddress, message: Message): Unit =
        server.sendMessage(address, message)
    }
  }

  override def sendMessage(address: InetSocketAddress, message: Message): Unit =
    throw new UnsupportedOperationException("The transport has not been started.")
}
