package io.iohk.cef.demo

import java.net.URI
import java.security.SecureRandom
import java.time.Clock
import java.util.UUID

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.{ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.{ByteString, Timeout}
import io.iohk.cef.db.DummyKnownNodesStorage
import io.iohk.cef.demo.SimpleNode3.{EchoReceived, NodeResponse, Resend, SendTo}
import io.iohk.cef.discovery.DiscoveryManager.{DiscoveredNodes, DiscoveryRequest, GetDiscoveredNodes}
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.transport.rlpx.RLPxConnectionHandler.RLPxConfiguration
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.Message.Version
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.{Message, MessageDecoder, MessageSerializable}
import io.iohk.cef.network.transport.rlpx.{AuthHandshaker, RLPxConnectionHandler, RLPxTransportProtocol}
import io.iohk.cef.network.{Capabilities, ECPublicKeyParametersNodeId, loadAsymmetricCipherKeyPair}
import io.iohk.cef.utils.RandomElement
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.duration._
import scala.util.Success

class SimpleNode3(nodeName: String, host: String, port: Int, bootstrapPeer: Option[URI]) {

  private val secureRandom: SecureRandom = new SecureRandom()
  private val nodeId = MantisCode.nodeIdFromNodeName(nodeName, secureRandom)
  private val nodeUri = new URI(s"enode://$nodeId@$host:$port")

  import SimpleNode3.{NodeCommand, Send, Start, Started}

  val server: Behavior[NodeCommand] = Behaviors.setup {
    context: ActorContext[NodeCommand] =>
      import akka.actor.typed.scaladsl.adapter._

      implicit val untypedActorSystem: ActorSystem = context.system.toUntyped

      val transport: RLPxTransportProtocol[String] =
        new RLPxTransportProtocol[String](MessageConfig.sampleEncoder, MessageConfig.sampleDecoder, rlpxProps(nodeName), IO(Tcp))

      val transportActor = context.spawn(transport.createTransport(), "RLPxTransport")

      import transport._

      case class UpdateConnectionCache(connectionCache: Map[URI, ActorRef[ConnectionCommand]]) extends NodeCommand


      val discoveryActor: ActorRef[DiscoveryRequest] =
        context.spawn(DiscoveryActor.discoveryBehavior(
          nodeUri, bootstrapPeer.fold(Set[URI]())(Set(_)), Capabilities(1),
          new DummyKnownNodesStorage(Clock.systemUTC())), "DiscoveryActor")

      def serverBehavior(timer: TimerScheduler[NodeCommand],
                         connectionCache: Map[URI, ActorRef[ConnectionCommand]],
                         serverListener: Option[ActorRef[NodeResponse]] = None): Behavior[NodeCommand] = Behaviors.setup {
        context =>

          def connectionHandlerFactory(context: ActorContext[NodeCommand]): () => ActorRef[ConnectionEvent] = () =>
            context.spawn(inboundConnectionBehavior, s"connection_${UUID.randomUUID().toString}")

          def inboundConnectionBehavior: Behavior[ConnectionEvent] =
            Behaviors.receiveMessage {
              case Connected(remoteUri, _) =>
                context.log.info(s"Inbound connection from $remoteUri")
                Behavior.same
              case ConnectionError(m, remoteUri, connection) =>
                context.log.info(s"Inbound connection failed from $remoteUri. $m")
                connection ! CloseConnection
                Behavior.stopped
              case MessageReceived(m, remoteUri, connection) =>
                context.log.info(s"Received: $m from $remoteUri")
                connection ! SendMessage(m) // echo back the message
                Behavior.same
              case ConnectionClosed(uri) =>
                context.log.info(s"Remote Connection closed by $uri")
                Behavior.stopped
            }

          def listenerBehaviour(replyTo: ActorRef[Started]): Behavior[ListenerEvent] = Behaviors.receiveMessage {
            case Listening(localUri, _) =>
              context.log.info(s"Server listening: $localUri")
              replyTo ! Started(localUri)
              Behavior.same
            case ListeningFailed(_, message) =>
              context.log.warning(message)
              Behavior.stopped
            case Unbound(_) =>
              context.log.info(s"Server unbound")
              Behavior.stopped
          }

          def outboundConnectionBehaviour(node: ActorRef[NodeCommand],
                                 connectionCont: (ActorContext[_], ActorRef[ConnectionCommand]) => Unit,
                                 maybeRemoteUri: Option[URI] = None):
          Behavior[ConnectionEvent] = Behaviors.receiveMessage {
            case Connected(remoteUri, connectionActor) =>
              context.log.info(s"Created new connection to $remoteUri")

              connectionCont(context, connectionActor) // now we have the connection, this sends the message

              node ! UpdateConnectionCache(connectionCache + (remoteUri -> connectionActor))

              outboundConnectionBehaviour(node, connectionCont, Some(remoteUri))

            case ConnectionError(m, remoteUri, connection) =>
              context.log.info(s"Failed to connect to $remoteUri. $m")
              Behavior.stopped
            case MessageReceived(m, remoteUri, connection) =>
              context.log.info(s"Received: $m from ${maybeRemoteUri.get}")
              serverListener.get ! EchoReceived(m)
              Behavior.same
            case ConnectionClosed(uri) =>
              context.log.info(s"Connection closed to $uri")
              Behavior.stopped
          }

          Behaviors.receiveMessage {
            case Start(replyTo) =>

              val listenerActor = context.spawn(listenerBehaviour(replyTo), "listener")

              transportActor ! CreateListener(nodeUri, listenerActor, connectionHandlerFactory(context))

              serverBehavior(timer, connectionCache, Some(replyTo))

            case UpdateConnectionCache(updatedCache) =>
              serverBehavior(timer, updatedCache, serverListener)

            case Send(msg) =>
              // run a discovery query and
              // if there are no peers,
              // schedule another send message for 5 seconds time
              implicit val timeout: Timeout = 1.second

              context.ask(discoveryActor)(GetDiscoveredNodes) {
                case Success(DiscoveredNodes(nodes)) =>
                  SendTo(msg, RandomElement.randomElement(nodes).node.toUri)
                case _ =>
                  Resend(Send(msg), 5 seconds)
              }

              Behavior.same

            case SendTo(msg, toUri) =>
              val withConnection: (ActorContext[_], ActorRef[ConnectionCommand]) => Unit =
                (context, connection) => {
                  context.log.info(s"Sending: $msg to $toUri")
                  connection ! SendMessage(msg)
                }

              // try to obtain an existing connection to the peer.
              // or create a connection and cache it
              // either way, run a continuation to actually send the message
              connectionCache.get(toUri).fold(
                transportActor ! Connect(
                  address = toUri,
                  eventHandler = context.spawn(outboundConnectionBehaviour(context.self, withConnection),
                    s"connection_handler_${UUID.randomUUID().toString}")))(withConnection(context, _))

              Behavior.same

            case Resend(msg, delay) =>
              context.schedule(delay, context.self, msg)
              Behavior.same
          }
      }

      Behaviors.withTimers(timer => serverBehavior(timer, Map()))
  }


  object MessageConfig {

    case class SampleMessage(content: String) extends MessageSerializable {
      override def toBytes(implicit di: DummyImplicit): ByteString = ByteString(content)

      override def toBytes: Array[Byte] = content.getBytes

      override def underlyingMsg: Message = this

      override def code: Version = 1
    }

    val sampleMessageDecoder = new MessageDecoder {
      override def fromBytes(`type`: Int, payload: Array[Byte], protocolVersion: Version): Message = SampleMessage(new String(payload))
    }

    val sampleEncoder: Encoder[String, ByteString] = ByteString(_)

    val sampleDecoder: Decoder[Message, String] = {
      case SampleMessage(content) => content
      case _ => throw new UnsupportedOperationException(s"This is a dummy test decoder and it only supports ${classOf[SampleMessage]}")
    }
  }

  object MantisCode {
    // mantis uses loadAsymmetricCipherKeyPair to lazily generate node keys
    def nodeKeyFromName(nodeName: String, secureRandom: SecureRandom): AsymmetricCipherKeyPair = {
      loadAsymmetricCipherKeyPair(s"/tmp/${nodeName}_key", secureRandom)
    }

    // how mantis generates node ids.
    def nodeIdFromKey(nodeKey: AsymmetricCipherKeyPair): String = {
      Hex.toHexString(new ECPublicKeyParametersNodeId(nodeKey.getPublic.asInstanceOf[ECPublicKeyParameters]).toNodeId)
    }

    def nodeIdFromNodeName(nodeName: String, secureRandom: SecureRandom): String =
      nodeIdFromKey(nodeKeyFromName(nodeName, secureRandom))
  }

  private def rlpxProps(nodeName: String): Props = {

    val rlpxConfiguration = new RLPxConfiguration {
      override val waitForHandshakeTimeout: FiniteDuration = 30 seconds
      override val waitForTcpAckTimeout: FiniteDuration = 30 seconds
    }

    val nodeKey: AsymmetricCipherKeyPair = MantisCode.nodeKeyFromName(nodeName, secureRandom)

    val authHandshaker = AuthHandshaker(nodeKey, secureRandom)

    RLPxConnectionHandler.props(
      MessageConfig.sampleMessageDecoder, protocolVersion = 1, authHandshaker, rlpxConfiguration)
  }
}

object SimpleNode3 {

  sealed trait NodeCommand

  case class Start(replyTo: ActorRef[NodeResponse]) extends NodeCommand

  case class Send(msg: String) extends NodeCommand

  private case class Resend(msg: NodeCommand, delay: FiniteDuration) extends NodeCommand

  private case class SendTo(msg: String, toUri: URI) extends NodeCommand

  sealed trait NodeResponse

  case class Started(nodeUri: URI) extends NodeResponse

  case class EchoReceived(msg: String) extends NodeResponse
}