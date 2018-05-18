package io.iohk.cef.net.transport.rlpx

import java.net.{InetSocketAddress, URI}
import java.util.UUID

import akka.{actor => untyped}
import untyped.Props
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.ByteString
import io.iohk.cef.encoding.rlp.EncodingAdapter
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.net.transport.rlpx.RLPxConnectionHandler.{ConnectTo, ConnectionEstablished, ConnectionFailed, HandleConnection}
import io.iohk.cef.net.transport.rlpx.ethereum.p2p.Message
import io.iohk.cef.net.transport.TransportProtocol

class RLPxTransportProtocol[T](encoder: Encoder[T, ByteString],
                               decoder: Decoder[Message, T],
                               rlpxConnectionHandlerProps: untyped.Props,
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

        case CreateListener(uri, replyTo, connectionFactory) =>
          context.spawn(listenerBehaviour(uri, replyTo, connectionFactory),
                        UUID.randomUUID().toString)
          Behavior.same
      }
    }

  private def listenerBehaviour(
      uri: URI,
      replyTo: ActorRef[ListenerEvent],
      connectionFactory: URI => ActorRef[ConnectionEvent]): Behavior[ListenerCommand] =
    Behaviors.setup { context =>
      val bridgeActor =
        context.actorOf(listenerBridge(uri, messageBehaviour, replyTo, connectionFactory))

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
            connectionBridgeActor ! io.iohk.cef.net.transport.rlpx.RLPxConnectionHandler
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
          rLPxConnectionHandler ! io.iohk.cef.net.transport.rlpx.RLPxConnectionHandler
            .SendMessage(new EncodingAdapter(m, encoder))
          Behavior.same
      }
    }

  private def listenerBridge(
      uri: URI,
      connectActor: untyped.ActorRef => Behavior[ConnectionCommand],
      replyTo: ActorRef[ListenerEvent],
      connectionFactory: URI => ActorRef[ConnectionEvent]): Props =
    ListenerBridge.props(uri,
                         tcpActor,
                         rlpxConnectionHandlerProps,
                         replyTo,
                         connectActor,
                        connectionFactory)

  private def connectionBridge(uri: URI,
                               connectActor: ActorRef[ConnectionCommand],
                               eventHandler: ActorRef[ConnectionEvent]): Props =
    ConnectionBridge.props(uri,
                           eventHandler,
                           rlpxConnectionHandlerProps,
                           connectActor)

  /**
    * Handles listener setup (i.e. the binding process).
    */
  class ListenerBridge(
      uri: URI,
      tcpActor: untyped.ActorRef,
      rlpxConnectionHandlerProps: untyped.Props,
      replyTo: ActorRef[ListenerEvent],
      connectBehaviour: untyped.ActorRef => Behavior[ConnectionCommand],
      connectionEventHandlerFactory: URI => ActorRef[ConnectionEvent])
      extends untyped.Actor {

    import akka.io.Tcp.{Bind, Bound, CommandFailed, Connected => TcpConnected}
    import akka.actor.typed.scaladsl.adapter._

    tcpActor ! Bind(self, toAddress(uri))

    override def receive: Receive = {

      case Bound(_) =>
        replyTo ! Listening(uri)

      case CommandFailed(b: Bind) => ???

      case TcpConnected(remoteAddr, localAddr) =>
        val rlpxConnectionHandler =
          context.actorOf(rlpxConnectionHandlerProps)

        val connectionActor: ActorRef[ConnectionCommand] =
          context.spawn(connectBehaviour(rlpxConnectionHandler), UUID.randomUUID().toString)

        context.actorOf(PeerBridge.props(remoteAddr, connectionEventHandlerFactory, connectionActor, rlpxConnectionHandler))
    }

    private def toAddress(uri: URI): InetSocketAddress =
      new InetSocketAddress(uri.getHost, uri.getPort)
  }

  object ListenerBridge {
    def props(uri: URI,
              tcpActor: untyped.ActorRef,
              rlpxConnectionHandlerProps: untyped.Props,
              replyTo: ActorRef[ListenerEvent],
              connectBehaviour: untyped.ActorRef => Behavior[ConnectionCommand],
              connectionFactory: URI => ActorRef[ConnectionEvent])
      : Props =
      Props(
        new ListenerBridge(uri,
                           tcpActor,
                           rlpxConnectionHandlerProps,
                           replyTo,
                           connectBehaviour,
                          connectionFactory))
  }

  /**
    * Bridge for handling an incoming connection and
    * accepting messages on that connection.
    */
  class PeerBridge(remoteAddress: InetSocketAddress,
                   connectionEventHandlerFactory: URI => ActorRef[ConnectionEvent],
                   connectionActor: ActorRef[ConnectionCommand],
                   rlpxConnectionHandler: untyped.ActorRef)
    extends untyped.Actor {

    rlpxConnectionHandler ! HandleConnection(sender())

    override def receive: Receive = awaitingConnectionHandshake

    private def awaitingConnectionHandshake: Receive = {

      case ConnectionEstablished(nodeId) =>
        val remoteUri = toUri(remoteAddress, nodeId)
        val connectionEventHandler = connectionEventHandlerFactory(remoteUri)
        context.become(connected(remoteUri, connectionEventHandler))

        connectionEventHandler ! Connected(toUri(remoteAddress, nodeId), connectionActor)

      case ConnectionFailed => ???
    }

    private def connected(uri: URI, connectionEventHandler: ActorRef[ConnectionEvent]): Receive = {
      case RLPxConnectionHandler.MessageReceived(message) => {
        val m: MessageType = decoder.decode(message)
        connectionEventHandler ! MessageReceived(m)
      }
    }

    private def toUri(remoteAddr: InetSocketAddress, nodeId: ByteString): URI =
      new URI(
        s"enode://${nodeId.utf8String}@${remoteAddr.getHostName}:${remoteAddr.getPort}")
  }

  object PeerBridge {
    def props(remoteAddress: InetSocketAddress,
              connectionEventHandlerFactory: URI => ActorRef[ConnectionEvent],
              connectionActor: ActorRef[ConnectionCommand],
              rlpxConnectionHandler: untyped.ActorRef): Props =
      Props(new PeerBridge(remoteAddress, connectionEventHandlerFactory, connectionActor, rlpxConnectionHandler))
  }

  /**
    * Bridge for setting up an outgoing connection and
    * sending/receiving messages on that connection.
    */
  class ConnectionBridge(uri: URI,
                         eventHandler: ActorRef[ConnectionEvent],
                         rlpxConnectionHandlerProps: untyped.Props,
                         connectBehaviour: ActorRef[ConnectionCommand])
      extends untyped.Actor {

    private val rlpxConnectionHandler =
      context.actorOf(rlpxConnectionHandlerProps)

    rlpxConnectionHandler ! ConnectTo(uri)

    override def receive: Receive = {
      case ConnectionEstablished(_) =>
        eventHandler ! Connected(uri, connectBehaviour)

      case ConnectionFailed =>
        eventHandler ! ConnectionError(s"Failed to connect to uri $uri", uri)

      case sm: RLPxConnectionHandler.SendMessage =>
        rlpxConnectionHandler ! sm

      case RLPxConnectionHandler.MessageReceived(message) => {
        val m: MessageType = decoder.decode(message)
        eventHandler ! MessageReceived(m)
      }
    }
  }

  object ConnectionBridge {
    def props(uri: URI,
              eventHandler: ActorRef[ConnectionEvent],
              rlpxConnectionHandlerProps: untyped.Props,
              connectBehaviour: ActorRef[ConnectionCommand]): Props =
      Props(
        new ConnectionBridge(uri,
                             eventHandler,
                             rlpxConnectionHandlerProps,
                             connectBehaviour))
  }
}
