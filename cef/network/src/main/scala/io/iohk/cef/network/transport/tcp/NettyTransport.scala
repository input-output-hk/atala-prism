package io.iohk.cef.network.transport.tcp

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import io.iohk.cef.network.encoding.StreamCodec
import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}

private[tcp] class NettyTransport[Message](address: InetSocketAddress,
                                           codec: StreamCodec[Message, ByteBuffer],
                                           messageHandler: (InetSocketAddress, Message) => Unit) {

  def start(): NettyTransport[Message] = {
    val bossGroup = new NioEventLoopGroup
    val workerGroup = new NioEventLoopGroup
    new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[SocketChannel]() {
        override def initChannel(ch: SocketChannel): Unit = {
          ch.pipeline().addLast(new NettyDecoder())
        }
      })
      .option[Integer](ChannelOption.SO_BACKLOG, 128)
      .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
      .bind(address)
      .await()
    this
  }

  def sendMessage(address: InetSocketAddress, message: Message): Unit = {

    val workerGroup = new NioEventLoopGroup()

    val activationAdapter = new ChannelInboundHandlerAdapter() {
      override def channelActive(ctx: ChannelHandlerContext): Unit = {
        val buf: ByteBuf = encode(message)
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
      })
      .connect(address)
  }

  private def encode(message: Message): ByteBuf =
    Unpooled.wrappedBuffer(codec.encoder.encode(message))


  private def decode(byteBuf: ByteBuf): Seq[Message] = {
    codec.decoder.decodeStream(byteBuf.nioBuffer())
  }

  private class NettyDecoder extends ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
      decode(msg.asInstanceOf[ByteBuf])
        .foreach(message => messageHandler(ctx.channel().remoteAddress().asInstanceOf[InetSocketAddress], message))
    }
  }
}
