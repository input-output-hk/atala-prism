package io.iohk.cef.discovery

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.agent.Agent
import akka.io.{IO, Udp}
import akka.util.ByteString
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus

class DiscoveryListener(
    discoveryConfig: DiscoveryConfig,
    nodeStatusHolder: Agent[NodeStatus],
    encoder: Encoder[DiscoveryMessage, ByteString],
    decoder: Decoder[ByteString, DiscoveryMessage])
  extends Actor with ActorLogging {

  import DiscoveryListener._
  import context.system

  override def receive: Receive = {
    case Start =>
      IO(Udp) ! Udp.Bind(self, new InetSocketAddress(discoveryConfig.interface, discoveryConfig.port))

    case Udp.Bound(local) =>
      context.parent ! Ready(local)
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {
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
            , nodeStatusHolder: Agent[NodeStatus]
            , encoder: Encoder[DiscoveryMessage, ByteString]
            , decoder: Decoder[ByteString, DiscoveryMessage]): Props =
    Props(new DiscoveryListener(config, nodeStatusHolder, encoder, decoder))

  case object Start

  case class Ready(address: InetSocketAddress)

  case class SendMessage(message: DiscoveryMessage, to: InetSocketAddress)

  case class MessageReceived(packet: DiscoveryMessage, from: InetSocketAddress)

  case class Blacklist()

}
