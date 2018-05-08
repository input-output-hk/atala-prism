package io.iohk.cef.net.rlpx

import java.net.URI

import akka.{actor => untyped}
import untyped.Props
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.ByteString
import io.iohk.cef.net.rlpx.RLPxConnectionHandler.{ConnectTo, ConnectionEstablished, ConnectionFailed}
import io.iohk.cef.net.transport.TransportProtocol
import io.iohk.cef.net.transport.TransportProtocol._

class RLPxTransportProtocol(rLPxConnectionHandlerProps: () => untyped.Props /* TODO a better way, surely? */) extends TransportProtocol {

  override type AddressType = URI
  override type PeerInfoType = ByteString // currently a byte string representing the peer's public key
  import akka.actor.typed.scaladsl.adapter._

  override def createTransport(): Behavior[TransportMessage[URI, ByteString]] = rlpxTransport(Map())

  private def rlpxTransport(connectionTable: Map[URI, untyped.ActorRef]): Behavior[TransportMessage[URI, ByteString]] =
    Behaviors.receive {
      (context, message) =>
        message match {
          case Connect(uri, replyTo) => {
            val connectHandlerActor = connectHandler(uri, replyTo)
            rlpxTransport(connectionTable + (uri -> context.actorOf(connectHandlerActor, s"handler_${uri.getUserInfo}")))
          }
        }
    }

  private def connectHandler(uri: URI, replyTo: ActorRef[ConnectionReply[ByteString]]): Props =
    ConnectHandler.props(uri, replyTo, rLPxConnectionHandlerProps)
}

class ConnectHandler(uri: URI, typedClient: ActorRef[ConnectionReply[ByteString]],
                     rlpxConnectionHandlerProps: () => untyped.Props) extends untyped.Actor {

  private val rlpxConnectionHandler = context.actorOf(rlpxConnectionHandlerProps())

  rlpxConnectionHandler ! ConnectTo(uri)

  override def receive: PartialFunction[Any, Unit] = {
    case ConnectionEstablished(nodeId: ByteString) => typedClient ! Connected(nodeId)
    case ConnectionFailed => typedClient ! ConnectionError(s"Failed to connect to uri '$uri'")
  }
}

object ConnectHandler {
  def props(uri: URI, typedClient: ActorRef[ConnectionReply[ByteString]],
            rlpxConnectionHandlerProps: () => untyped.Props): Props =
    Props(new ConnectHandler(uri, typedClient, rlpxConnectionHandlerProps))
}
