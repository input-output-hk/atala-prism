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

class RLPxTransportProtocol(rLPxConnectionHandlerProps: () => untyped.Props /* TODO a better way? */,
                            tcpActor: untyped.ActorRef) extends TransportProtocol {

  override type AddressType = URI
  override type PeerInfoType = ByteString // currently a byte string representing the peer's public key
  import akka.actor.typed.scaladsl.adapter._

  override def createTransport(): Behavior[TransportCommand[URI, ByteString]] = rlpxTransport(Map(), Map())

  private def rlpxTransport(connectionTable: Map[URI, untyped.ActorRef], listenerTable: Map[String, ActorRef[ListenerCommand[URI, ByteString]]]):
  Behavior[TransportCommand[URI, ByteString]] =
    Behaviors.receive {
      (context, message) =>
        message match {

          case Connect(uri, replyTo) =>
            val connectHandlerActor = connectHandler(uri, replyTo)
            rlpxTransport(connectionTable + (uri -> context.actorOf(connectHandlerActor)), listenerTable)

          case CreateListener(replyTo) =>
            val listenerId = UUID.randomUUID().toString
            val listenerActor: ActorRef[ListenerCommand[URI, ByteString]] = context.spawn(listenerBehaviour, listenerId)
            replyTo ! ListenerCreated(listenerActor)
            rlpxTransport(connectionTable, listenerTable + (listenerId -> listenerActor))
        }
    }

  private val listenerBehaviour: Behavior[ListenerCommand[URI, ByteString]] = Behaviors.receive {
    (context, message) =>
      message match {
        case Listen(uri, replyTo) =>
          context.actorOf(bindHandler(uri, replyTo))
          Behavior.same
      }
  }

  private def bindHandler(uri: URI, replyTo: ActorRef[ListenerEvent[URI, ByteString]]): Props =
    BindHandler.props(uri, tcpActor, rLPxConnectionHandlerProps, replyTo)

  private def connectHandler(uri: URI, replyTo: ActorRef[ConnectionReply[ByteString]]): Props =
    ConnectHandler.props(uri, replyTo, rLPxConnectionHandlerProps)
}

class BindHandler(uri: URI,
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

    case c @ Connected(remote, local) =>
      rlpxConnectionHandler ! HandleConnection(sender())
      context.become(awaitingConnectionHandshake)
  }

  private def awaitingConnectionHandshake: Receive = {
    case ConnectionEstablished(nodeId) => replyTo ! ConnectionReceived(nodeId)
    case ConnectionFailed => ???
  }

  private def toAddress(uri: URI): InetSocketAddress =
    new InetSocketAddress(uri.getHost(), uri.getPort)
}

object BindHandler {
  def props(uri: URI, tcpActor: untyped.ActorRef,
            rlpxConnectionHandlerProps: () => untyped.Props,
            replyTo: ActorRef[ListenerEvent[URI, ByteString]]): Props =
    Props(new BindHandler(uri, tcpActor, rlpxConnectionHandlerProps, replyTo))
}

class ConnectHandler(uri: URI, typedClient: ActorRef[ConnectionReply[ByteString]],
                     rlpxConnectionHandlerProps: () => untyped.Props) extends untyped.Actor {

  private val rlpxConnectionHandler = context.actorOf(rlpxConnectionHandlerProps())

  rlpxConnectionHandler ! ConnectTo(uri)

  override def receive: Receive = {
    case ConnectionEstablished(nodeId: ByteString) => typedClient ! Connected(nodeId)
    case ConnectionFailed => typedClient ! ConnectionError(s"Failed to connect to uri $uri", ByteString(uri.getUserInfo))
  }
}

object ConnectHandler {
  def props(uri: URI, typedClient: ActorRef[ConnectionReply[ByteString]],
            rlpxConnectionHandlerProps: () => untyped.Props): Props =
    Props(new ConnectHandler(uri, typedClient, rlpxConnectionHandlerProps))
}
