package io.iohk.cef.net.rlpx

import java.net.{InetSocketAddress, URI}
import java.util.UUID

import akka.{actor => untyped}
import untyped.Props
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.ByteString
import io.iohk.cef.encoding.rlp.EncodingAdapter
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.net.rlpx.RLPxConnectionHandler.{ConnectTo, ConnectionEstablished, ConnectionFailed, HandleConnection}
import io.iohk.cef.net.rlpx.ethereum.p2p.Message
import io.iohk.cef.net.transport.TransportProtocol

class RLPxTransportProtocol[T](encoder: Encoder[T, Array[Byte]],
                               decoder: Decoder[Array[Byte], T],
                               rlpxConnectionHandlerProps: () => untyped.Props,
                               tcpActor: untyped.ActorRef)
    extends TransportProtocol {

  override type AddressType = URI
  override type MessageType = T

  import akka.actor.typed.scaladsl.adapter._

  override def createTransport(): Behavior[TransportCommand] =
    rlpxTransport()

  // top-level transport behaviour accepts messages to
  // create outbound connections or listen for inbound connections.
  private def rlpxTransport(): Behavior[TransportCommand] =
    Behaviors.receive { (context, message) =>
      message match {

        case Connect(uri, eventHandler) =>
          context.spawn(connectionBehaviour(uri, eventHandler),
                        UUID.randomUUID().toString)
          Behavior.same

        case CreateListener(uri, replyTo) =>
          context.spawn(listenerBehaviour(uri, replyTo),
                        UUID.randomUUID().toString)
          Behavior.same
      }
    }

  private def listenerBehaviour(
      uri: URI,
      replyTo: ActorRef[ListenerEvent]): Behavior[ListenerCommand] =
    Behaviors.setup { context =>
      val bridgeActor =
        context.actorOf(listenerBridge(uri, messageBehaviour, replyTo))

      Behaviors.receive { (context, message) =>
        message match { // TODO unbind the listener
          case _ => Behavior.same
        }
      }
    }

  private def connectionBehaviour(
      uri: URI,
      eventHandler: ActorRef[ConnectionEvent]): Behavior[ConnectionCommand] =
    Behaviors.setup { context =>
      val connectionBridgeActor =
        context.actorOf(connectionBridge(uri, context.self, eventHandler))

      Behaviors.receive { (_, message) =>
        message match {
          case SendMessage(m) =>
            connectionBridgeActor ! io.iohk.cef.net.rlpx.RLPxConnectionHandler
              .SendMessage(new EncodingAdapter(m, encoder))
            Behavior.same
        }
      }
    }

  private def messageBehaviour(
      rLPxConnectionHandler: untyped.ActorRef): Behavior[ConnectionCommand] =
    Behaviors.receive { (_, message) =>
      message match {
        case SendMessage(m) =>
          rLPxConnectionHandler ! io.iohk.cef.net.rlpx.RLPxConnectionHandler
            .SendMessage(new EncodingAdapter(m, encoder))
          Behavior.same
      }
    }

  private def listenerBridge(
      uri: URI,
      connectActor: untyped.ActorRef => Behavior[ConnectionCommand],
      replyTo: ActorRef[ListenerEvent]): Props =
    ListenerBridge.props(uri,
                         tcpActor,
                         rlpxConnectionHandlerProps,
                         replyTo,
                         connectActor)

  private def connectionBridge(uri: URI,
                               connectActor: ActorRef[ConnectionCommand],
                               eventHandler: ActorRef[ConnectionEvent]): Props =
    ConnectionBridge.props(uri,
                           eventHandler,
                           rlpxConnectionHandlerProps,
                           connectActor)

  class ListenerBridge(
      uri: URI,
      tcpActor: untyped.ActorRef,
      rlpxConnectionHandlerProps: () => untyped.Props,
      replyTo: ActorRef[ListenerEvent],
      connectBehaviour: untyped.ActorRef => Behavior[ConnectionCommand])
      extends untyped.Actor {

    import akka.io.Tcp.{Bind, Bound, CommandFailed, Connected => TcpConnected}
    import akka.actor.typed.scaladsl.adapter._

    private val rlpxConnectionHandler =
      context.actorOf(rlpxConnectionHandlerProps())
    private val connectHandler =
      context.spawn(connectBehaviour(rlpxConnectionHandler), "something")

    tcpActor ! Bind(self, toAddress(uri))

    override def receive: Receive = binding

    private def binding: Receive = {

      case Bound(_) =>
        replyTo ! Listening(uri)

      case CommandFailed(b: Bind) => ???

      case c @ TcpConnected(remoteAddr, localAddr) =>
        rlpxConnectionHandler ! HandleConnection(sender())
        context.become(awaitingConnectionHandshake(remoteAddr))
    }

    private def awaitingConnectionHandshake(
        remoteAddr: InetSocketAddress): Receive = {
      case ConnectionEstablished(nodeId) =>
        replyTo ! ConnectionReceived(toUri(remoteAddr, nodeId), connectHandler)
      case ConnectionFailed => ???
    }

    private def toAddress(uri: URI): InetSocketAddress =
      new InetSocketAddress(uri.getHost, uri.getPort)

    private def toUri(remoteAddr: InetSocketAddress, nodeId: ByteString): URI =
      new URI(
        s"enode://${nodeId.utf8String}@${remoteAddr.getHostName}:${remoteAddr.getPort}")

  }

  object ListenerBridge {
    def props(uri: URI,
              tcpActor: untyped.ActorRef,
              rlpxConnectionHandlerProps: () => untyped.Props,
              replyTo: ActorRef[ListenerEvent],
              connectBehaviour: untyped.ActorRef => Behavior[ConnectionCommand])
      : Props =
      Props(
        new ListenerBridge(uri,
                           tcpActor,
                           rlpxConnectionHandlerProps,
                           replyTo,
                           connectBehaviour))
  }

  /**
    * Bridges between the typed connectionBehaviour and the untyped RlpxConnectionHandler.
    */
  class ConnectionBridge(uri: URI,
                         eventHandler: ActorRef[ConnectionEvent],
                         rlpxConnectionHandlerProps: () => untyped.Props,
                         connectBehaviour: ActorRef[ConnectionCommand])
      extends untyped.Actor {

    private val rlpxConnectionHandler =
      context.actorOf(rlpxConnectionHandlerProps())

    rlpxConnectionHandler ! ConnectTo(uri)

    override def receive: Receive = {
      case ConnectionEstablished(_) =>
        eventHandler ! Connected(uri, connectBehaviour)

      case ConnectionFailed =>
        eventHandler ! ConnectionError(s"Failed to connect to uri $uri", uri)

      case sm: RLPxConnectionHandler.SendMessage =>
        rlpxConnectionHandler ! sm

      case mr@RLPxConnectionHandler.MessageReceived(message) => {
        val m: MessageType = decode(message)
        eventHandler ! MessageReceived(m)
      }
    }

    private def decode(message: Message): MessageType = ??? //decoder.decode(message.)
  }

  object ConnectionBridge {
    def props(uri: URI,
              eventHandler: ActorRef[ConnectionEvent],
              rlpxConnectionHandlerProps: () => untyped.Props,
              connectBehaviour: ActorRef[ConnectionCommand]): Props =
      Props(
        new ConnectionBridge(uri,
                             eventHandler,
                             rlpxConnectionHandlerProps,
                             connectBehaviour))
  }
}
