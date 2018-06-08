package io.iohk.cef.network.transport.rlpx

import java.net.{InetSocketAddress, URI}
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.{actor => untyped}
import untyped.Props
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.io.Tcp.Register
import akka.util.ByteString
import io.iohk.cef.encoding.rlp.EncodingAdapter
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.transport.rlpx.RLPxConnectionHandler.{ConnectTo, ConnectionEstablished, ConnectionFailed, HandleConnection}
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.Message
import io.iohk.cef.network.transport.TransportProtocol
import io.iohk.cef.telemetery.RegistryConfig
import org.bouncycastle.util.encoders.Hex

class RLPxTransportProtocol[T](encoder: Encoder[T, ByteString],
                               decoder: Decoder[Message, T],
                               rlpxConnectionHandlerProps: untyped.Props,
                               tcpActor: untyped.ActorRef)
    extends TransportProtocol {

  override type AddressType = URI
  override type MessageType = T

  import akka.actor.typed.scaladsl.adapter._

  val connectionsTracker = RegistryConfig.registry.gauge("connections", new AtomicInteger())

  override def createTransport(): Behavior[TransportCommand] =
    rlpxTransport()

  // top-level transport behaviour accepts messages to
  // create outbound connections or listen for inbound connections.
  private def rlpxTransport(): Behavior[TransportCommand] =
    Behaviors.receive { (context, message) =>
      message match {

        case Connect(uri, replyTo, eventHandler) =>
          context.spawn(connectionBehaviour(uri, replyTo, eventHandler),
                        UUID.randomUUID().toString)
          Behavior.same

        case CreateListener(uri, replyTo, connectionFactory) =>
          context.spawn(listenerBehaviour(uri, replyTo, connectionFactory),
                        UUID.randomUUID().toString)
          Behavior.same
      }
    }

  // manages individual socket listeners and handles Unbind requests
  // for those listeners.
  private def listenerBehaviour(
      uri: URI,
      replyTo: ActorRef[ListenerEvent],
      connectionFactory: URI => ActorRef[ConnectionEvent]): Behavior[ListenerCommand] =
    Behaviors.setup { context =>

      val bridgeActor =
        context.actorOf(listenerBridge(uri, connectionBehaviour, replyTo, context.self, connectionFactory))

      Behaviors.receiveMessage {
        case Unbind =>
          bridgeActor ! Unbind
          Behavior.stopped
      }
    }

  // manages outbound connections and handles SendMessage requests
  // for those connections.
  private def connectionBehaviour(
      uri: URI,
      replyTo: ActorRef[ActorRef[ConnectionCommand]],
      eventHandler: ActorRef[ConnectionEvent]): Behavior[ConnectionCommand] = Behaviors.setup {

    context =>

      val connectionBridgeActor =
        context.actorOf(connectionBridge(uri, context.self, eventHandler))

      replyTo ! context.self

      Behaviors.receiveMessage {
        case SendMessage(m) =>
          connectionBridgeActor ! RLPxConnectionHandler.SendMessage(new EncodingAdapter(m, encoder))
          Behavior.same
        case CloseConnection =>
          connectionBridgeActor ! akka.io.Tcp.Close
          Behavior.ignore // The bridge will stop this actor upon receiving the Tcp close event.
      }
    }

  // handles message sending for inbound connections to listeners
  private def connectionBehaviour(rLPxConnectionHandler: untyped.ActorRef): Behavior[ConnectionCommand] =
    Behaviors.receiveMessage {
      case SendMessage(m) =>
        rLPxConnectionHandler ! RLPxConnectionHandler.SendMessage(new EncodingAdapter(m, encoder))
        Behavior.same
      case CloseConnection =>
        ???
    }

  private def listenerBridge(
      uri: URI,
      connectActor: untyped.ActorRef => Behavior[ConnectionCommand],
      replyTo: ActorRef[ListenerEvent],
      listenerActor: ActorRef[ListenerCommand],
      connectionFactory: URI => ActorRef[ConnectionEvent]): Props =
    ListenerBridge.props(uri,
                         tcpActor,
                         rlpxConnectionHandlerProps,
                         replyTo,
                         listenerActor,
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
                        listenerActor: ActorRef[ListenerCommand],
                        connectBehaviour: untyped.ActorRef => Behavior[ConnectionCommand],
                        connectionEventHandlerFactory: URI => ActorRef[ConnectionEvent])
      extends untyped.Actor {

    import akka.io.Tcp.{Bind, Bound, CommandFailed, Connected => TcpConnected}

    tcpActor ! Bind(self, toAddress(uri))

    override def receive: Receive = {

      case Bound(_) =>
        val socket = sender()
        replyTo ! Listening(uri, listenerActor)
        context.become(bound(socket))

      case CommandFailed(_: Bind) =>
        replyTo ! ListeningFailed(uri, s"Error setting up listener on $uri")
        context.stop(self)
    }

    def bound(socket: untyped.ActorRef): Receive = {

      case TcpConnected(remoteAddr, localAddr) =>

        val tcpConnection = sender()

        context.actorOf(
          PeerBridge.props(
            remoteAddr, connectionEventHandlerFactory,
            rlpxConnectionHandlerProps, tcpConnection,
            connectBehaviour))

      case Unbind =>
        socket ! akka.io.Tcp.Unbind
        replyTo ! Unbound(uri)

    }

    private def toAddress(uri: URI): InetSocketAddress =
      new InetSocketAddress(uri.getHost, uri.getPort)
  }

  object ListenerBridge {
    def props(uri: URI,
              tcpActor: untyped.ActorRef,
              rlpxConnectionHandlerProps: untyped.Props,
              replyTo: ActorRef[ListenerEvent],
              listenerActor: ActorRef[ListenerCommand],
              connectBehaviour: untyped.ActorRef => Behavior[ConnectionCommand],
              connectionFactory: URI => ActorRef[ConnectionEvent])
      : Props =
      Props(
        new ListenerBridge(uri,
                           tcpActor,
                           rlpxConnectionHandlerProps,
                           replyTo,
                            listenerActor,
                           connectBehaviour,
                          connectionFactory))
  }

  /**
    * Bridge for handling an incoming connection and
    * accepting messages on that connection.
    */
  class PeerBridge(remoteAddress: InetSocketAddress,
                   connectionEventHandlerFactory: URI => ActorRef[ConnectionEvent],
                   rlpxConnectionHandlerProps: untyped.Props,
                   tcpConnection: untyped.ActorRef,
                   connectBehaviour: untyped.ActorRef => Behavior[ConnectionCommand])
    extends untyped.Actor {

    private val rlpxConnectionHandler =
      context.actorOf(rlpxConnectionHandlerProps)

    private val connectionActor: ActorRef[ConnectionCommand] =
      context.spawn(connectBehaviour(rlpxConnectionHandler), UUID.randomUUID().toString)

    tcpConnection ! Register(rlpxConnectionHandler)

    rlpxConnectionHandler ! HandleConnection(tcpConnection)

    override def receive: Receive = {

      case ConnectionEstablished(nodeId) =>
        val remoteUri = toUri(remoteAddress, nodeId)
        val connectionEventHandler = connectionEventHandlerFactory(remoteUri)
        connectionsGauge.incrementAndGet()
        context.become(connected(remoteUri, connectionEventHandler))

        connectionEventHandler ! Connected(remoteUri, connectionActor)

      case ConnectionFailed =>
        val remoteUri = toUri(remoteAddress, ByteString())
        val connectionEventHandler = connectionEventHandlerFactory(remoteUri)
        connectionEventHandler ! ConnectionError(s"Error setting up connection with peer at $remoteAddress", remoteUri)
        context.stop(self)
    }

    private def connected(uri: URI, connectionEventHandler: ActorRef[ConnectionEvent]): Receive = {
      case RLPxConnectionHandler.MessageReceived(message) => {
        val m: MessageType = decoder.decode(message)
        connectionEventHandler ! MessageReceived(m)
      }
    }

    private def toUri(remoteAddr: InetSocketAddress, nodeId: ByteString): URI =
      new URI(
        s"enode://${Hex.toHexString(nodeId.toArray)}@${remoteAddr.getHostName}:${remoteAddr.getPort}")
  }

  object PeerBridge {
    def props(remoteAddress: InetSocketAddress,
              connectionEventHandlerFactory: URI => ActorRef[ConnectionEvent],
              rlpxConnectionHandlerProps: untyped.Props,
              tcpConnection: untyped.ActorRef,
              connectBehaviour: untyped.ActorRef => Behavior[ConnectionCommand]): Props =
      Props(new PeerBridge(remoteAddress, connectionEventHandlerFactory,
        rlpxConnectionHandlerProps, tcpConnection, connectBehaviour))
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
      context.actorOf(rlpxConnectionHandlerProps, s"conn-bridge-rlpx-handler-${UUID.randomUUID().toString}")

    rlpxConnectionHandler ! ConnectTo(uri)

    override def receive: Receive = {

      case ConnectionEstablished(_) =>
        eventHandler ! Connected(uri, connectBehaviour)

      case ConnectionFailed =>
        eventHandler ! ConnectionError(s"Failed to connect to uri $uri", uri)
        context.stop(self)

      case sm: RLPxConnectionHandler.SendMessage =>
        rlpxConnectionHandler ! sm

      case RLPxConnectionHandler.MessageReceived(message) =>
        eventHandler ! MessageReceived(decoder.decode(message))

      case akka.io.Tcp.Close =>
        rlpxConnectionHandler ! akka.io.Tcp.Close
        context.become(awaitingClosure)
    }

    private def awaitingClosure: Receive = {
        case akka.io.Tcp.Closed =>
          eventHandler ! ConnectionClosed(uri)
          context.stop(connectBehaviour)
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
