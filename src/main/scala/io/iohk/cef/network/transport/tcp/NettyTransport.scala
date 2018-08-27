package io.iohk.cef.network.transport.tcp

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList

import io.iohk.cef.network.encoding.nio.StreamCodecs.MessageApplication
import io.iohk.cef.network.encoding.nio.{NioDecoder, _}
import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}

import scala.collection.JavaConverters._

private[network] class NettyTransport(address: InetSocketAddress) {

  private val messageApplications = new CopyOnWriteArrayList[MessageApplication[InetSocketAddress]]()

  new ServerBootstrap()
    .group(new NioEventLoopGroup, new NioEventLoopGroup)
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

  def withMessageApplication[Message](decoder: NioDecoder[Message],
                                      handler: (InetSocketAddress, Message) => Unit): Unit =
    messageApplications.add(StreamCodecs.lazyMessageApplication(decoder, handler))

  def sendMessage(address: InetSocketAddress, message: ByteBuffer): Unit = {

    val workerGroup = new NioEventLoopGroup()

    val activationAdapter = new ChannelInboundHandlerAdapter() {
      override def channelActive(ctx: ChannelHandlerContext): Unit = {
        try {
          ctx.writeAndFlush(Unpooled.wrappedBuffer(message))
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

  private class NettyDecoder extends ChannelInboundHandlerAdapter {
    override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
      val remoteAddress = ctx.channel().remoteAddress().asInstanceOf[InetSocketAddress]

      StreamCodecs
        .decodeStream(remoteAddress, msg.asInstanceOf[ByteBuf].nioBuffer(), messageApplications.asScala)
        .foreach(_.apply)
    }
  }
}
