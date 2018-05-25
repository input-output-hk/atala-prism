package io.iohk.cef.discovery

import java.net.InetSocketAddress

import akka.io.{IO, Udp}
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.encoding.{Decoder, Encoder}

class DiscoveryListener(
                         discoveryConfig: DiscoveryConfig,
                         encoder: Encoder[DiscoveryWireMessage, ByteString],
                         decoder: Decoder[ByteString, DiscoveryWireMessage])
  extends untyped.Actor with untyped.ActorLogging {

  import DiscoveryListener._
  import context.system

  override def receive: Receive = {
    case Start =>
      IO(Udp) ! Udp.Bind(self, new InetSocketAddress(discoveryConfig.interface, discoveryConfig.port))

    case Udp.Bound(local) =>
      context.parent ! Ready(local)
      context.become(ready(sender()))
  }

  def ready(socket: untyped.ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      val packet = decoder.decode(data)
      val msgReceived = MessageReceived(packet, remote)

      context.parent ! msgReceived

    case SendMessage(packet, to) =>
      val encodedPacket = encoder.encode(packet)
      socket ! Udp.Send(encodedPacket, to)
  }
}

object DiscoveryListener {
  def props(config: DiscoveryConfig
            , encoder: Encoder[DiscoveryWireMessage, ByteString]
            , decoder: Decoder[ByteString, DiscoveryWireMessage]): untyped.Props =
    untyped.Props(new DiscoveryListener(config, encoder, decoder))

  case object Start

  case class Ready(address: InetSocketAddress)

  case class SendMessage(message: DiscoveryWireMessage, to: InetSocketAddress)

  case class MessageReceived(packet: DiscoveryWireMessage, from: InetSocketAddress)

  case class Blacklist()

}
