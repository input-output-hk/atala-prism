package io.iohk.cef.demo

import java.net.{InetSocketAddress, URI}
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, Logger}
import akka.io.{IO, Tcp}
import akka.util.{ByteString, Timeout}
import io.iohk.cef.discovery.db.{KnownNode, KnownNodeStorage}
import io.iohk.cef.demo.SimpleNode3.{Confirmed, NodeResponse, Resend, SendTo}
import io.iohk.cef.discovery.DiscoveryConfig
import io.iohk.cef.discovery.DiscoveryManager.{DiscoveredNodes, DiscoveryRequest, GetDiscoveredNodes}
import io.iohk.cef.network.transport.rlpx.RLPxTransportProtocol
import io.iohk.cef.network.{Capabilities, NodeInfo}
import io.iohk.cef.telemetery.DatadogTelemetry
import io.micrometer.core.instrument.{Counter, DistributionSummary}
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.duration._
import scala.util.Success

class SimpleNode3(node: NodeInfo,
                  transport: RLPxTransportProtocol[String],
                  knownNodeStorage: KnownNodeStorage,
                  discoveryConfig: DiscoveryConfig) extends DatadogTelemetry {

  private val nodeUri = node.getServerUri

  import SimpleNode3.{NodeCommand, Send, Start, Started}

  val inboundConnGauge = registry.gauge("connections.inbound", new AtomicInteger(0))
  val outboundConnGauge = registry.gauge("connections.outbound", new AtomicInteger(0))
  val messageReceivedCounter = registry.counter("messages.count.received")
  val messageSentCounter = registry.counter("messages.count.sent")
  val messageSentSize = DistributionSummary
    .builder("messages.size.sent")
    .baseUnit("bytes")
    .register(registry)
  val messageReceivedSize = DistributionSummary
    .builder("messages.size.received")
    .baseUnit("bytes")
    .register(registry)

  val server: Behavior[NodeCommand] = Behaviors.setup {
    context: ActorContext[NodeCommand] =>
      import akka.actor.typed.scaladsl.adapter._

      implicit val untypedActorSystem: ActorSystem = context.system.toUntyped

      val transportActor = context.spawn(transport.createTransport(), "RLPxTransport")

      import transport._

      def recordMessage(messageType: MessageType, counter: Counter, sizeSummary: DistributionSummary) = {
        counter.increment()
        sizeSummary.record(messageType.getBytes().size)
      }

      def recordIncomingMessage(message: MessageType) = {
        recordMessage(message, messageReceivedCounter, messageReceivedSize)
      }

      def recordOutgoingMessage(message: MessageType) = {
        recordMessage(message, messageSentCounter, messageSentSize)
      }

      case class UpdateConnectionCache(connectionCache: Map[URI, ActorRef[ConnectionCommand]]) extends NodeCommand
      case class NotifyMessageTracker(message: String, receivedBy: URI) extends NodeCommand

      val discoveryBehavior = DiscoveryActor.discoveryBehavior(
        node, discoveryConfig, knownNodeStorage)

      val discoveryActor: ActorRef[DiscoveryRequest] =
        context.spawn(discoveryBehavior, "DiscoveryActor")

      def serverBehavior(timer: TimerScheduler[NodeCommand],
                         connectionCache: Map[URI, ActorRef[ConnectionCommand]],
                         messageTracker: Map[String, (Int, Set[URI])],
                         serverListener: Option[ActorRef[NodeResponse]] = None): Behavior[NodeCommand] = Behaviors.setup {
        context =>

          def connectionHandlerFactory(context: ActorContext[NodeCommand]): () => ActorRef[ConnectionEvent] = () =>
            context.spawn(inboundConnectionBehavior, s"connection_${UUID.randomUUID().toString}")

          def inboundConnectionBehavior: Behavior[ConnectionEvent] =
            Behaviors.receiveMessage {
              case Connected(remoteUri, _) =>
                context.log.info(s"Inbound connection from $remoteUri")
                inboundConnGauge.incrementAndGet()
                Behavior.same
              case ConnectionError(m, remoteUri, connection) =>
                context.log.info(s"Inbound connection failed from $remoteUri. $m")
                connection ! CloseConnection
                Behavior.stopped
              case MessageReceived(m, remoteUri, connection) =>
                recordIncomingMessage(m)
                context.log.info(s"Received/confirmed: $m from $remoteUri")
                connection ! SendMessage(m) // echo back the message
                Behavior.same
              case ConnectionClosed(uri) =>
                context.log.info(s"Remote Connection closed by $uri")
                inboundConnGauge.decrementAndGet()
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
                                 connectionCont: (Logger, URI, ActorRef[ConnectionCommand]) => Unit):
          Behavior[ConnectionEvent] = Behaviors.receiveMessage {
            case Connected(remoteUri, connectionActor) =>
              context.log.info(s"Created new connection to $remoteUri")

              connectionCont(context.log, remoteUri, connectionActor) // now we have the connection, this sends the message

              node ! UpdateConnectionCache(connectionCache + (remoteUri -> connectionActor))
              outboundConnGauge.incrementAndGet()

              Behavior.same

            case ConnectionError(m, remoteUri, _) =>
              context.log.info(s"Failed to connect to $remoteUri. $m")
              Behavior.stopped
            case MessageReceived(m, remoteUri, _) =>
              recordIncomingMessage(m)
              context.log.info(s"Confirmed: $m by $remoteUri")

              node ! NotifyMessageTracker(m, remoteUri)

              Behavior.same

            case ConnectionClosed(uri) =>
              context.log.info(s"Connection closed to $uri")
              outboundConnGauge.decrementAndGet()
              Behavior.stopped
          }

          Behaviors.receiveMessage {
            case Start(replyTo) =>

              val listenerActor = context.spawn(listenerBehaviour(replyTo), "listener")

              transportActor ! CreateListener(nodeUri, listenerActor, connectionHandlerFactory(context))

              serverBehavior(timer, connectionCache, messageTracker, Some(replyTo))

            case UpdateConnectionCache(updatedCache) =>
              serverBehavior(timer, updatedCache, messageTracker, serverListener)

            case NotifyMessageTracker(message: String, receivedBy: URI) =>
              val (peerCount, currentUris): (Int, Set[URI]) = messageTracker(message)
              val remainingUris = currentUris - receivedBy
              val newMessageTracker =
                if (remainingUris.isEmpty) {
                  serverListener.get ! Confirmed(message, peerCount)
                  messageTracker - message
                } else {
                  messageTracker + (message -> (peerCount, remainingUris))
                }

              serverBehavior(timer, connectionCache, newMessageTracker, serverListener)

            case Send(msg) =>
              // run a discovery query and
              // if there are no peers,
              // schedule another send message for 5 seconds time
              implicit val timeout: Timeout = 1.second

              context.ask(discoveryActor)(GetDiscoveredNodes) {
                case Success(DiscoveredNodes(nodes: Set[KnownNode])) =>
                  SendTo(msg, nodes.filter(SimpleNode3.notDead).map(_.node.getServerUri))
                case _ =>
                  Resend(Send(msg), 5 seconds)
              }

              Behavior.same

            case SendTo(msg, toUris) =>
              def withConnection: (Logger, URI, ActorRef[ConnectionCommand]) => Unit =
                (logger, uri, connection) => {
                  logger.info(s"Sending: $msg to $uri")
                  recordOutgoingMessage(msg)
                  connection ! SendMessage(msg)
                }

              context.log.info(s"Peer size is ${toUris.size} for sending $msg ($toUris).")

              if (toUris.nonEmpty) {
                // try to obtain an existing connection to the peer.
                // or create a connection and cache it
                // either way, run a continuation to actually send the message
                toUris.foreach(toUri =>
                  connectionCache.get(toUri).fold(
                    transportActor ! Connect(
                      address = toUri,
                      eventHandler = context.spawn(outboundConnectionBehaviour(context.self, withConnection),
                        s"connection_handler_${UUID.randomUUID().toString}")))(withConnection(context.log, toUri, _))
                )

                val newMessageTracker = messageTracker + (msg -> (toUris.size, toUris))

                serverBehavior(timer, connectionCache, newMessageTracker, serverListener)
              } else {
                serverListener.get ! Confirmed(msg, 0)
                Behavior.same
              }

            case Resend(msg, delay) =>
              context.schedule(delay, context.self, msg)
              Behavior.same
          }
      }

      Behaviors.withTimers(timer => serverBehavior(timer, Map(), Map(), None))
  }
}

object SimpleNode3 {

  sealed trait NodeCommand

  case class Start(replyTo: ActorRef[NodeResponse]) extends NodeCommand

  case class Send(msg: String) extends NodeCommand

  private case class Resend(msg: NodeCommand, delay: FiniteDuration) extends NodeCommand

  private case class SendTo(msg: String, toUris: Set[URI]) extends NodeCommand

  sealed trait NodeResponse

  case class Started(nodeUri: URI) extends NodeResponse

  case class Confirmed(msg: String, peerCount: Int) extends NodeResponse

  def notDead(knownNode: KnownNode): Boolean =
    knownNode.lastSeen.plusSeconds(10).isAfter(Instant.now)

  def apply(host: String, port: Int, nodeKey: String, discoveryConfig: DiscoveryConfig, knownNodeStorage: KnownNodeStorage)(
    implicit actorSystem: ActorSystem): SimpleNode3 = {

    import io.iohk.cef.crypto._
    val nodeKeyBytes: Array[Byte] = Hex.decode(nodeKey)
    val keyPair: AsymmetricCipherKeyPair = keyPairFromPrvKey(nodeKeyBytes)
    val nodeId = ByteString(MantisCode.nodeIdFromKey(keyPair))

    val nodeInfo = NodeInfo(
      id = nodeId,
      discoveryAddress = new InetSocketAddress(discoveryConfig.interface, discoveryConfig.port),
      serverAddress = new InetSocketAddress(host, port),
      capabilities = Capabilities(1))

    new SimpleNode3(
      nodeInfo,
      new RLPxTransportProtocol[String](
        MessageConfig.sampleEncoder,
        MessageConfig.sampleDecoder,
        MantisCode.rlpxProps(keyPair), IO(Tcp)),
      knownNodeStorage,
      discoveryConfig
    )
  }

  def apply(nodeName: String, host: String, port: Int, discoveryConfig: DiscoveryConfig, knownNodeStorage: KnownNodeStorage)(implicit actorSystem: ActorSystem): SimpleNode3 = {

    val keyPair: AsymmetricCipherKeyPair = MantisCode.nodeKeyFromName(nodeName)

    val (priv, _) = io.iohk.cef.crypto.keyPairToByteArrays(keyPair)

    SimpleNode3(host, port, Hex.toHexString(priv), discoveryConfig, knownNodeStorage)
  }
}