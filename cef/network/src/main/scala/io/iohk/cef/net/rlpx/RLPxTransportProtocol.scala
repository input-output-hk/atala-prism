package io.iohk.cef.net.rlpx

import java.net.URI

import akka.{actor => untyped}
import untyped.Props
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.ByteString
import io.iohk.cef.net.rlpx.RLPxConnectionHandler.{ConnectTo, ConnectionEstablished, ConnectionFailed, RLPxConfiguration}
import io.iohk.cef.net.rlpx.ethereum.p2p.MessageDecoder
import io.iohk.cef.net.transport.TransportProtocol
import io.iohk.cef.net.transport.TransportProtocol._

class RLPxTransportProtocol(messageDecoder: MessageDecoder, protocolVersion: Int,
                            authHandshaker: AuthHandshaker, rlpxConfiguration: RLPxConfiguration) extends TransportProtocol {

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
    ConnectHandler.props(uri, replyTo, messageDecoder, protocolVersion, authHandshaker, rlpxConfiguration)
}

class ConnectHandler(uri: URI, typedClient: ActorRef[ConnectionReply[ByteString]],
                     messageDecoder: MessageDecoder, protocolVersion: Int,
                     authHandshaker: AuthHandshaker, rlpxConfiguration: RLPxConfiguration) extends untyped.Actor {
    import context.system
    private val rlpxConnectionHandler = system.actorOf(RLPxConnectionHandler.props(messageDecoder, protocolVersion, authHandshaker, rlpxConfiguration))

    rlpxConnectionHandler ! ConnectTo(uri)

  override def receive: PartialFunction[Any, Unit] = {
    case ConnectionEstablished(nodeId: ByteString) => typedClient ! Connected(nodeId)
    case ConnectionFailed => typedClient ! ConnectionError(s"Failed to connect to uri '$uri'")
  }
}

object ConnectHandler {
  def props(uri: URI, typedClient: ActorRef[ConnectionReply[ByteString]],
            messageDecoder: MessageDecoder, protocolVersion: Int,
            authHandshaker: AuthHandshaker, rlpxConfiguration: RLPxConfiguration): Props =
    Props(new ConnectHandler(uri, typedClient, messageDecoder, protocolVersion, authHandshaker, rlpxConfiguration))
}
