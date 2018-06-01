package io.iohk.cef.discovery

import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.{Clock, Instant}

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.ByteString
import io.iohk.cef.crypto
import io.iohk.cef.db.KnownNodesStorage
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.ServerStatus
import io.iohk.cef.utils.FiniteSizedMap
import DiscoveryListener._
import org.bouncycastle.util.encoders.Hex

import scala.util.Random


object DiscoveryManager {

  import io.iohk.cef.db.KnownNode
  import io.iohk.cef.network.Node

  sealed trait DiscoveryRequest

  case class Blacklist(node: Node) extends DiscoveryRequest

  case class GetDiscoveredNodes(replyTo: ActorRef[DiscoveredNodes]) extends DiscoveryRequest

  case class FetchNeighbors(node: Node) extends DiscoveryRequest

  private[discovery] case class DiscoveryResponseWrapper(innerMessage: DiscoveryListenerResponse) extends DiscoveryRequest

  private [discovery] case object Scan extends DiscoveryRequest


  sealed trait DiscoveryResponse

  case class DiscoveredNodes(nodes: Set[KnownNode]) extends DiscoveryResponse

  private [discovery] sealed trait NodeEvent {
    def timestamp: Instant
  }

  private [discovery] case class Sought(node: Node, timestamp: Instant) extends NodeEvent

  private [discovery] case class Pinged(node: Node, timestamp: Instant) extends NodeEvent

  private val nonceSize = 2

  def behaviour(discoveryConfig: DiscoveryConfig,
                knownNodesStorage: KnownNodesStorage,
                nodeState: NodeState,
                clock: Clock,
                encoder: Encoder[DiscoveryWireMessage, ByteString],
                decoder: Decoder[ByteString, DiscoveryWireMessage],
                discoveryListenerFactory: ActorContext[DiscoveryRequest] => ActorRef[DiscoveryListenerRequest],
                randomSource: SecureRandom): Behavior[DiscoveryRequest] = Behaviors.setup {
    context =>

      import akka.actor.typed.scaladsl.adapter._

      val pingedNodes: FiniteSizedMap[ByteString, Pinged] =
        FiniteSizedMap(discoveryConfig.concurrencyDegree, discoveryConfig.messageExpiration * 2, clock)

      val soughtNodes: FiniteSizedMap[ByteString, Sought] =
        FiniteSizedMap(discoveryConfig.concurrencyDegree, discoveryConfig.messageExpiration * 2, clock)

      val buffer = StashBuffer[DiscoveryRequest](capacity = 100)

      val discoveryListener = discoveryListenerFactory(context)

      val discoveryListenerAdapter = context.messageAdapter(DiscoveryResponseWrapper)

      def startListening(timer: TimerScheduler[DiscoveryRequest]): Behavior[DiscoveryRequest] = Behaviors.setup {
        context =>

          discoveryListener ! Start(discoveryListenerAdapter)

          Behaviors.receiveMessage {
            case DiscoveryResponseWrapper(Ready(address)) =>
              context.log.debug(
                s"UDP address $address bound successfully. " +
                s"Pinging ${discoveryConfig.bootstrapNodes.size} bootstrap Nodes.")

              discoveryConfig.bootstrapNodes.foreach(node =>
                sendPing(discoveryListener, address, node)
              )

              timer.startPeriodicTimer("scan_timer", Scan, discoveryConfig.scanInterval)

              // processess all stashed messages with listening
              // and also returns listening as the current behaviour
              buffer.unstashAll(context, listening(address))
            case msg =>
              buffer.stash(msg)
              Behaviors.same
          }
      }

      def listening(address: InetSocketAddress): Behavior[DiscoveryRequest] = Behaviors.receiveMessage {

        case Blacklist(node: Node) =>
          knownNodesStorage.blacklist(node, discoveryConfig.blacklistDefaultDuration)
          Behavior.same

        case GetDiscoveredNodes(replyTo) =>
          replyTo ! DiscoveredNodes(knownNodesStorage.getAll())
          Behavior.same

        case FetchNeighbors(node: Node) =>
          sendPing(discoveryListener, address, node)
          Behavior.same

        case DiscoveryResponseWrapper(innerMessage: DiscoveryListener.MessageReceived) =>
          processDiscoveryMessage(address, innerMessage)
          Behavior.same

        case Scan =>
          scan(address)
          Behavior.same

        case x => throw new IllegalStateException(s"Received an unexpected message '$x'.")
      }

      def sendPing(listener: ActorRef[DiscoveryListenerRequest], listeningAddress: InetSocketAddress, node: Node): Unit = {
        context.log.debug(s"Sending ping to ${node.toUri}")
        val nonce = new Array[Byte](nonceSize)
        randomSource.nextBytes(nonce)
        val ping = Ping(DiscoveryWireMessage.ProtocolVersion, getNode(listeningAddress), expirationTimestamp, ByteString(nonce))
        val key = calculateMessageKey(encoder, ping)
        context.log.debug(s"Ping message: ${ping}")
        pingedNodes.put(key, Pinged(node, clock.instant())).foreach(pinged => {
          listener ! DiscoveryListener.SendMessage(ping, pinged.node.discoveryAddress)
        })
      }

      def scan(address: InetSocketAddress): Unit = {
        context.log.debug("Starting peer scan.")
        val expired = pingedNodes.dropExpired

        // Eliminating the nodes that never answered.
        expired.foreach {
          case (id, pingInfo) => {
            context.log.debug(s"Dropping node ${Hex.toHexString(id.toArray)}")
            pingedNodes -= id
            knownNodesStorage.remove(pingInfo.node)
          }
        }

        new Random().shuffle(pingedNodes.values).take(discoveryConfig.scanNodesLimit).foreach { pingInfo =>
          sendPing(discoveryListener, address, pingInfo.node)
        }

        knownNodesStorage.getAll().toSeq
          .sortBy(_.lastSeen)
          .takeRight(discoveryConfig.scanNodesLimit)
          .foreach { nodeInfo =>
            sendPing(discoveryListener, address, nodeInfo.node)
          }
      }

      def getServerAddress(default: InetSocketAddress): InetSocketAddress = nodeState.serverStatus match {
        case ServerStatus.Listening(addr) => addr
        case _ => default
      }

      def getNode(discoveryAddress: InetSocketAddress): Node =
        Node(nodeState.nodeId, discoveryAddress, getServerAddress(default = discoveryAddress), nodeState.capabilities)

      def hasNotExpired(timestamp: Long): Boolean =
        timestamp > clock.instant().getEpochSecond

      def expirationTimestamp: Long =
        clock.instant().plusSeconds(discoveryConfig.messageExpiration.toSeconds).getEpochSecond

      def processDiscoveryMessage(address: InetSocketAddress, message: DiscoveryListener.MessageReceived): Unit = message match {
        case DiscoveryListener.MessageReceived(ping@Ping(protocolVersion, sourceNode, timestamp, _), from) =>
          if (hasNotExpired(timestamp) &&
            protocolVersion == DiscoveryWireMessage.ProtocolVersion) {
            context.log.debug(s"Received a ping message from ${from}, replyTo: ${sourceNode.discoveryAddress}")
            val messageKey = calculateMessageKey(encoder, ping)
            val node = getNode(discoveryAddress = address)
            val pong = Pong(node, messageKey, expirationTimestamp)
            if (sourceNode.capabilities.satisfies(nodeState.capabilities)) {
              knownNodesStorage.insert(sourceNode)
              context.log.debug(s"New discovered list: ${knownNodesStorage.getAll().map(_.node.discoveryAddress)}")
            }
            context.log.debug(s"Sending pong message with capabilities ${pong.node.capabilities}, to: ${sourceNode.discoveryAddress}")
            discoveryListener ! DiscoveryListener.SendMessage(pong, sourceNode.discoveryAddress)
          } else {
            context.log.warning(s"Received an invalid Ping message")
          }
        case DiscoveryListener.MessageReceived(Pong(pingedNode, token, timestamp), from) =>
          if (hasNotExpired(timestamp)) {
            context.log.debug(s"Received pong from $pingedNode with token ${Hex.encode(token.toArray)}")
            pingedNodes.get(token).foreach { _ =>
              context.log.debug(s"Received a pong message from ${from}")
              pingedNodes -= token
              if (pingedNode.capabilities.satisfies(nodeState.capabilities)) {
                context.system.toUntyped.eventStream.publish(CompatibleNodeFound(pingedNode))
                knownNodesStorage.insert(pingedNode)
                context.log.debug(s"New discovered list: ${knownNodesStorage.getAll().map(_.node.discoveryAddress)}")
              } else {
                context.log.debug(s"Node received in pong ${from} does not satisfy the capabilities.")
              }
              val nonce = new Array[Byte](nonceSize)
              randomSource.nextBytes(nonce)
              val seek = Seek(nodeState.capabilities, discoveryConfig.maxSeekResults, expirationTimestamp, ByteString(nonce))
              context.log.debug(s"Sending seek message ${seek} to ${pingedNode.discoveryAddress}")
              discoveryListener ! DiscoveryListener.SendMessage(seek, pingedNode.discoveryAddress)
              val messageKey = calculateMessageKey(encoder, seek)
              soughtNodes.put(messageKey, Sought(pingedNode, clock.instant()))
            }
          } else {
            context.log.warning("Received an invalid Pong message")
          }

        case DiscoveryListener.MessageReceived(seek@Seek(capabilities, maxResults, timestamp, _), from) =>
          if (hasNotExpired(timestamp)) {
            context.log.debug(s"Received a seek message ${seek} from ${from}")
            val nodes =
              knownNodesStorage.getAll().filter(_.node.capabilities.satisfies(capabilities)).map(_.node)
            context.log.debug(s"Nodes are ${knownNodesStorage.getAll()}")
            context.log.debug(s"Nodes satisfying capabilities = ${nodes.map(_.discoveryAddress)}")
            val randomNodeSubset = Random.shuffle(nodes).take(maxResults)
            val token = calculateMessageKey(encoder, seek)
            val neighbors = Neighbors(capabilities, token, nodes.size, randomNodeSubset.toSeq, expirationTimestamp)
            context.log.debug(s"Sending neighbors answer with ${randomNodeSubset} to ${from}")
            discoveryListener ! DiscoveryListener.SendMessage(neighbors, from)
          } else {
            context.log.warning("Received an invalid Seek message")
          }
        case DiscoveryListener.MessageReceived(Neighbors(capabilities, token, _, neighbors, timestamp), from) =>
          if (hasNotExpired(timestamp) &&
            capabilities.satisfies(nodeState.capabilities)) {
            context.log.debug(s"Received a neighbors message from ${from}. Neighbors: $neighbors")
            val discoveredNodes = knownNodesStorage.getAll().map(_.node).union(pingedNodes.values.map(_.node).toSet)
            soughtNodes.get(token).foreach { _ =>
              val newNodes = if (discoveryConfig.multipleConnectionsPerAddress) {
                val nodeEndpoints = discoveredNodes.map(dn => (ByteString(dn.discoveryAddress.getAddress.getAddress), dn.discoveryAddress.getPort))
                neighbors.filterNot(node => nodeEndpoints.contains((ByteString(node.discoveryAddress.getAddress.getAddress), node.discoveryAddress.getPort)))
              } else {
                val nodeEndpoints = discoveredNodes.map(dn => ByteString(dn.discoveryAddress.getAddress.getAddress))
                neighbors.filterNot(node => nodeEndpoints.contains(ByteString(node.discoveryAddress.getAddress.getAddress)))
              }
              val newNodesWithoutMe = newNodes.filterNot(_.id == nodeState.nodeId)
              context.log.debug(s"Sending ping to ${newNodesWithoutMe.size} nodes. Nodes: ${newNodesWithoutMe.map(_.discoveryAddress)}")
              newNodesWithoutMe.foreach(node => {
                sendPing(discoveryListener, address, node)
              })
              soughtNodes -= token
            }
          } else {
            context.log.warning("Received an invalid Neighbors message")
          }
      }

      Behaviors.withTimers(startListening)
  }

  def calculateMessageKey(encoder: Encoder[DiscoveryWireMessage, ByteString], message: DiscoveryWireMessage): ByteString = {
    val encodedPing = encoder.encode(message)
    crypto.kec256(encodedPing)
  }
}
