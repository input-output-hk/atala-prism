package io.iohk.cef.network.discovery

import java.net.InetSocketAddress

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import akka.{actor => untyped}
import akka.io.{IO, Udp}
import akka.util.ByteString
import io.iohk.cef.network.discovery.DiscoveryListener._
import io.iohk.cef.encoding.{Decoder, Encoder}
import akka.actor.typed.scaladsl.adapter._

class UDPBridge(
    discoveryListener: ActorRef[DiscoveryListenerRequest],
    encoder: Encoder[DiscoveryWireMessage, ByteString],
    decoder: Decoder[ByteString, DiscoveryWireMessage],
    udpBinder: untyped.ActorContext => Unit)
    extends untyped.Actor {

  udpBinder(context)

  override def receive: Receive = {
    case Udp.Bound(local) =>
      discoveryListener ! Forward(Ready(local))
      context.become(ready(sender()))
  }

  private def ready(socket: untyped.ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      decoder.decode(data).foreach(packet => discoveryListener ! Forward(MessageReceived(packet, remote)))

    case SendMessage(packet, to) =>
      val encodedPacket = encoder.encode(packet)
      socket ! Udp.Send(encodedPacket, to)
  }
}

object UDPBridge {
  def creator(
      config: DiscoveryConfig,
      encoder: Encoder[DiscoveryWireMessage, ByteString],
      decoder: Decoder[ByteString, DiscoveryWireMessage])(
      context: ActorContext[DiscoveryListenerRequest]): untyped.ActorRef =
    context.actorOf(
      untyped.Props(new UDPBridge(
        context.asScala.self,
        encoder,
        decoder,
        (context) =>
          IO(Udp)(context.system)
            .tell(Udp.Bind(context.self, new InetSocketAddress(config.interface, config.port)), context.self)
      )))
}
