package io.iohk.cef.demo

import java.net.URI
import java.security.SecureRandom
import java.time.Clock
import java.util.UUID

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.{ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import io.iohk.cef.db.DummyKnownNodesStorage
import io.iohk.cef.discovery.DiscoveryManager.{DiscoveredNodes, DiscoveryRequest, DiscoveryResponse, GetDiscoveredNodes}
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.transport.rlpx.RLPxConnectionHandler.RLPxConfiguration
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.Message.Version
import io.iohk.cef.network.transport.rlpx.ethereum.p2p.{Message, MessageDecoder, MessageSerializable}
import io.iohk.cef.network.transport.rlpx.{AuthHandshaker, RLPxConnectionHandler, RLPxTransportProtocol}
import io.iohk.cef.network.{Capabilities, ECPublicKeyParametersNodeId, loadAsymmetricCipherKeyPair}
import io.iohk.cef.utils.{RandomElement, UtilityBehaviors}
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.duration._

class SimpleNode3(nodeName: String, host: String, port: Int, bootstrapPeer: Option[URI]) {

  private val secureRandom: SecureRandom = new SecureRandom()
  private val nodeId = MantisCode.nodeIdFromNodeName(nodeName, secureRandom)
  private val nodeUri = new URI(s"enode://$nodeId@$host:$port")

  import SimpleNode3.{NodeCommand, Send, Start, Started}

  val server: Behavior[NodeCommand] = Behaviors.setup { context: ActorContext[NodeCommand] =>
    import akka.actor.typed.scaladsl.adapter._

    implicit val untypedActorSystem: ActorSystem = context.system.toUntyped

    val transport: RLPxTransportProtocol[String] =
      new RLPxTransportProtocol[String](MessageConfig.sampleEncoder, MessageConfig.sampleDecoder, rlpxProps(nodeName), IO(Tcp))

    import transport._

    case class UpdateConnectionCache(connectionCache: Map[URI, ActorRef[ConnectionCommand]]) extends NodeCommand


    def connectionHandlerFactory(context: ActorContext[NodeCommand])(remoteUri: URI): ActorRef[ConnectionEvent] = {
      context.spawn(connectionBehaviour(remoteUri), s"connection_${UUID.randomUUID().toString}")
    }

    def connectionBehaviour(remoteUri: URI): Behavior[ConnectionEvent] = Behaviors.receiveMessage {
      case Connected(remoteUri, connectionActor) =>
        println(s"I got an inbound connection from $remoteUri")
        Behavior.same
      case ConnectionError(m, remoteUri) =>
        println(s"Inbound connection failed from $remoteUri")
        Behavior.stopped
      case MessageReceived(m) =>
        println(s"I received message $m from $remoteUri")
        Behavior.same
      case ConnectionClosed(uri) =>
        println(s"Remote Connection closed by $uri")
        Behavior.stopped
    }

    val transportActor = context.spawn(transport.createTransport(), "RLPxTransport")

    val discoveryActor: ActorRef[DiscoveryRequest] =
      context.spawn(DiscoveryActor.discoveryBehavior(
        nodeUri, bootstrapPeer.fold(Set[URI]())(Set(_)), Capabilities(1),
        new DummyKnownNodesStorage(Clock.systemUTC())), "DiscoveryActor")

    def serverBehavior(timer: TimerScheduler[NodeCommand], connectionCache: Map[URI, ActorRef[ConnectionCommand]]): Behavior[NodeCommand] = Behaviors.setup {
      context =>

        def listenerBehaviour(replyTo: ActorRef[Started]): Behavior[ListenerEvent] = Behaviors.receiveMessage {
          case Listening(localUri, _) =>
            context.log.info(s"Server listening: $localUri")
            replyTo ! Started(localUri)
            Behavior.same
          case ListeningFailed(_, message) =>
            println(message)
            Behavior.stopped
          case Unbound(_) =>
            println(s"Server unbound")
            Behavior.stopped
        }

        def connectedBehaviour(node: ActorRef[NodeCommand],
                               connectionCont: ActorRef[ConnectionCommand] => Unit): Behavior[ConnectionEvent] = Behaviors.receiveMessage {
          case Connected(remoteUri, connectionActor) =>
            println(s"Successfully connected to $remoteUri")

            connectionCont(connectionActor) // now we have the connection, this sends the message

            node ! UpdateConnectionCache( connectionCache + (remoteUri -> connectionActor))

            Behavior.stopped

          case ConnectionError(m, remoteUri) =>
            println(s"Failed to connect to $remoteUri")
            Behavior.stopped
          case MessageReceived(m) =>
            context.log.warning("Received unexpected message on an outbound message channel.")
            Behavior.stopped
          case ConnectionClosed(uri) =>
            context.log.debug(s"Connection closed by uri $uri")
            Behavior.stopped
        }


        Behaviors.receiveMessage {
          case Start(replyTo) =>

            val listenerActor = context.spawn(listenerBehaviour(replyTo), "listener")

            transportActor ! CreateListener(nodeUri, listenerActor, connectionHandlerFactory(context))

            Behavior.same

          case UpdateConnectionCache(updatedCache) =>
            serverBehavior(timer, updatedCache)

          case Send(msg) =>
            // run a discovery query and
            // if there are no peers,
            // schedule another send message for 5 seconds time

            def discoveryQuery(node: ActorRef[NodeCommand]): Behavior[DiscoveryResponse] = Behaviors.setup {
              context =>

                discoveryActor ! GetDiscoveredNodes(context.self)

                Behaviors.receiveMessage({
                  case DiscoveredNodes(nodes) =>
                    if (nodes.nonEmpty) {

                      // select a peer randomly
                      val toUri = RandomElement.randomElement(nodes).node.toUri

                      val withConnection: ActorRef[ConnectionCommand] => Unit =
                        connection => connection ! SendMessage(msg)

                      // try to obtain an existing connection to the peer.
                      // or create a connection and cache it
                      connectionCache.get(toUri).fold(
                        transportActor ! Connect(
                          toUri,
                          UtilityBehaviors.ignore(context),
                          context.spawn(
                            connectedBehaviour(node, withConnection),
                            s"connection_handler_${UUID.randomUUID().toString}")))(
                        (connectionActor: ActorRef[ConnectionCommand]) => connectionActor ! SendMessage(msg))

                    } else {
                      timer.startSingleTimer("retry_timer", Send(msg), 5 seconds)
                    }
                    Behavior.same
                })
            }

            context.spawn(discoveryQuery(context.self), s"discovery_query_${UUID.randomUUID().toString}")

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

  case class Start(replyTo: ActorRef[Started]) extends NodeCommand

  case class Send(msg: String) extends NodeCommand

  case class Started(nodeUri: URI)

}