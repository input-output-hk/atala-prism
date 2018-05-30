package io.iohk.cef.discovery

import java.net.InetSocketAddress

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.io.{IO, Udp}
import akka.actor.typed.scaladsl.adapter._
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.encoding.{Decoder, Encoder}

private [cef] object DiscoveryListener {

  sealed trait DiscoveryListenerRequest

  case class Start(replyTo: ActorRef[DiscoveryListenerResponse]) extends DiscoveryListenerRequest

  case class SendMessage(message: DiscoveryWireMessage, to: InetSocketAddress) extends DiscoveryListenerRequest

  private case class Forward(response: DiscoveryListenerResponse) extends DiscoveryListenerRequest

  sealed trait DiscoveryListenerResponse

  case class Ready(address: InetSocketAddress) extends DiscoveryListenerResponse

  case class MessageReceived(packet: DiscoveryWireMessage, from: InetSocketAddress) extends DiscoveryListenerResponse



  def behavior(discoveryConfig: DiscoveryConfig,
               encoder: Encoder[DiscoveryWireMessage, ByteString],
               decoder: Decoder[ByteString, DiscoveryWireMessage]): Behavior[DiscoveryListenerRequest] = {

    class UDPBridge(discoveryListener: ActorRef[DiscoveryListenerRequest]) extends untyped.Actor {

      IO(Udp)(context.system) ! Udp.Bind(self, new InetSocketAddress(discoveryConfig.interface, discoveryConfig.port))

      override def receive: Receive = {
        case Udp.Bound(local) =>
          discoveryListener ! Forward(Ready(local))
          context.become(ready(sender()))
      }

      private def ready(socket: untyped.ActorRef): Receive = {
        case Udp.Received(data, remote) =>
          val packet = decoder.decode(data)
          discoveryListener ! Forward(MessageReceived(packet, remote))

        case SendMessage(packet, to) =>
          val encodedPacket = encoder.encode(packet)
          socket ! Udp.Send(encodedPacket, to)
      }
    }

    def initialState: Behavior[DiscoveryListenerRequest] = Behaviors.receivePartial {
      case (context, Start(replyTo)) => {
        val udpBridge = context.actorOf(untyped.Props(new UDPBridge(context.self)))
        startingState(replyTo, udpBridge)
      }
      case (context, msg) => {
        context.log.warning(
          s"Ignoring discovery listener request $msg. " +
            s"The discovery listener is not yet initialzed. " +
            s"You need to send a Start message.")
        Behavior.same
      }
    }

    def startingState(replyTo: ActorRef[DiscoveryListenerResponse], udpBridge: untyped.ActorRef): Behavior[DiscoveryListenerRequest] =
      Behaviors.setup {
        context =>
          Behaviors.receiveMessage {
            case Start(_) =>
              throw new IllegalStateException(
                s"Start has already been invoked on this listener by actor $replyTo. " +
                  s"You can do this only once.")

            case Forward(ready: Ready) =>
              replyTo ! ready
              readyState(replyTo, udpBridge)

            case x =>
              context.log.warning(
                s"Ignoring discovery listener request $x. " +
                  s"The discovery listener is starting. " +
                  s"You need to await the Ready message.")
              Behavior.same
          }
      }

    def readyState(replyTo: ActorRef[DiscoveryListenerResponse], udpBridge: untyped.ActorRef): Behavior[DiscoveryListenerRequest] =
      Behaviors.setup {
        context =>
          Behaviors.receiveMessage {
            case Forward(messageReceived: MessageReceived) =>
              replyTo ! messageReceived
              Behavior.same
            case sm: SendMessage =>
              udpBridge ! sm
              Behaviors.same
            case x =>
              context.log.warning(
                s"Ignoring discovery listener request $x. " +
                  s"The discovery listener is already listening and ready.")
              Behaviors.same
          }
      }

    initialState
  }
}


