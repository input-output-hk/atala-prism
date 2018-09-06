package io.iohk.cef.network.transport.tcp

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.UUID

import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.transport.{MonixStreamLike, NetworkTransport, StreamLike}
import monix.execution.Cancelable
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, OverflowStrategy}

private[transport] class TcpNetworkTransport[Message](nettyTransport: NettyTransport)(
    implicit encoder: NioEncoder[Message],
    decoder: NioDecoder[Message])
    extends NetworkTransport[InetSocketAddress, Message] {

  val monixMessageStream: Observable[Message] =
    Observable.create(overflowStrategy = OverflowStrategy.Unbounded)((subscriber: Subscriber.Sync[Message]) => {

      def msgHandler(address: InetSocketAddress, message: Message): Unit = {
        subscriber.onNext(message)
      }

      val applicationId = nettyTransport.withMessageApplication(decoder, msgHandler)

      cancelableMessageApplication(applicationId)
    })

  val messageStream: StreamLike[Message] = new MonixStreamLike[Message](monixMessageStream)

  override def sendMessage(address: InetSocketAddress, message: Message): Unit =
    nettyTransport.sendMessage(address, encode(message))

  private def cancelableMessageApplication(applicationId: UUID): Cancelable =
    () => nettyTransport.cancelMessageApplication(applicationId)

  private def encode(message: Message): ByteBuffer =
    encoder.encode(message)
}
