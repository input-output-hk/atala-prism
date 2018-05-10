package io.iohk.cef.net.rlpx

import java.net.{InetSocketAddress, URI}
import java.util.UUID

import akka.{actor => untyped}
import untyped.Props
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.ByteString
import io.iohk.cef.net.rlpx.RLPxConnectionHandler.{ConnectTo, ConnectionEstablished, ConnectionFailed, HandleConnection}
import io.iohk.cef.net.transport.TransportProtocol
import io.iohk.cef.net.transport.TransportProtocol._

class RLPxTransportProtocol(rLPxConnectionHandlerProps: () => untyped.Props,
                            tcpActor: untyped.ActorRef) extends TransportProtocol {

  override type AddressType = URI
  override type PeerInfoType = ByteString // currently a byte string representing the peer's public key
  import akka.actor.typed.scaladsl.adapter._

  override def createTransport(): Behavior[TransportCommand[URI, ByteString]] = rlpxTransport()

  private def rlpxTransport(): Behavior[TransportCommand[URI, ByteString]] =
    Behaviors.receive {
      (context, message) =>
        message match {

          case Connect(uri, replyTo) =>
            context.spawn(connectionBehaviour(uri, replyTo), UUID.randomUUID().toString)
            Behavior.same

          case CreateListener(replyTo) =>
            context.spawn(listenerBehaviour(replyTo), UUID.randomUUID().toString)
            Behavior.same
        }
    }

  private def listenerBehaviour(replyTo: ActorRef[ListenerCreated[URI, ByteString]]): Behavior[ListenerCommand[URI, ByteString]] = Behaviors.setup {
    context =>

      replyTo ! ListenerCreated(context.self)

      Behaviors.receive {
        (context, message) =>
          message match {
            case Listen(uri, replyToBind) =>
              context.actorOf(binderBridge(uri, replyToBind))
              Behavior.same
          }
      }
  }

  private def connectionBehaviour(uri: URI, replyTo: ActorRef[ConnectionReply[ByteString]]): Behavior[ConnectionCommand] = Behaviors.setup {
    context =>

      val connectionBridgeActor = context.actorOf(connectionBridge(uri, context.self, replyTo))

      Behaviors.receive {
        (context, message) =>
          message match {
            case SendMessage(messageSerializable) =>
              connectionBridgeActor ! io.iohk.cef.net.rlpx.RLPxConnectionHandler.SendMessage(messageSerializable)
              Behavior.same
          }
      }
  }

  private def binderBridge(uri: URI, replyTo: ActorRef[ListenerEvent[URI, ByteString]]): Props =
    BinderBridge.props(uri, tcpActor, rLPxConnectionHandlerProps, replyTo)

  private def connectionBridge(uri: URI, connectActor: ActorRef[ConnectionCommand], replyTo: ActorRef[ConnectionReply[ByteString]]): Props =
    ConnectionBridge.props(uri, replyTo, rLPxConnectionHandlerProps, connectActor)
}

/**
  * Bridges between the typed listener behaviour and untyped TCP and RlpxConnectionHandler actors.
  */
class BinderBridge(uri: URI,
                   tcpActor: untyped.ActorRef,
                   rlpxConnectionHandlerProps: () => untyped.Props,
                   replyTo: ActorRef[ListenerEvent[URI, ByteString]]) extends untyped.Actor {

  import akka.io.Tcp.{Bind, Bound, CommandFailed, Connected}

  private val rlpxConnectionHandler = context.actorOf(rlpxConnectionHandlerProps())

  tcpActor ! Bind(self, toAddress(uri))

  override def receive: Receive = binding

  private def binding: Receive = {

    case Bound(_) =>
      replyTo ! Listening(uri)

    case CommandFailed(b: Bind) => ???

    case c@Connected(remote, local) =>
      rlpxConnectionHandler ! HandleConnection(sender())
      context.become(awaitingConnectionHandshake)
  }

  private def awaitingConnectionHandshake: Receive = {
    case ConnectionEstablished(nodeId) => replyTo ! ConnectionReceived(nodeId)
    case ConnectionFailed => ???
  }

  private def toAddress(uri: URI): InetSocketAddress =
    new InetSocketAddress(uri.getHost, uri.getPort)
}

object BinderBridge {
  def props(uri: URI, tcpActor: untyped.ActorRef,
            rlpxConnectionHandlerProps: () => untyped.Props,
            replyTo: ActorRef[ListenerEvent[URI, ByteString]]): Props =
    Props(new BinderBridge(uri, tcpActor, rlpxConnectionHandlerProps, replyTo))
}

/**
  * Bridges between the typed connectionBehaviour and the untyped RlpxConnectionHandler.
  */
class ConnectionBridge(uri: URI, typedClient: ActorRef[ConnectionReply[ByteString]],
                       rlpxConnectionHandlerProps: () => untyped.Props,
                       connectBehaviour: ActorRef[ConnectionCommand]) extends untyped.Actor {

  private val rlpxConnectionHandler = context.actorOf(rlpxConnectionHandlerProps())

  rlpxConnectionHandler ! ConnectTo(uri)

  override def receive: Receive = {
    case ConnectionEstablished(nodeId: ByteString) =>
      typedClient ! Connected(nodeId, connectBehaviour)

    case ConnectionFailed => typedClient ! ConnectionError(s"Failed to connect to uri $uri", ByteString(uri.getUserInfo))

    case sm: io.iohk.cef.net.rlpx.RLPxConnectionHandler.SendMessage =>
      rlpxConnectionHandler ! sm
  }
}

object ConnectionBridge {
  def props(uri: URI, typedClient: ActorRef[ConnectionReply[ByteString]],
            rlpxConnectionHandlerProps: () => untyped.Props,
            connectBehaviour: ActorRef[ConnectionCommand]): Props =
    Props(new ConnectionBridge(uri, typedClient, rlpxConnectionHandlerProps, connectBehaviour))
}
