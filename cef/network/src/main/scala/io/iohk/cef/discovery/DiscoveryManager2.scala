package io.iohk.cef.discovery

import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.{Clock, Instant}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer}
import akka.util.{ByteString, Timeout}
import akka.{actor => untyped}
import akka.pattern.ask
import io.iohk.cef.crypto
import io.iohk.cef.db.KnownNodesStorage
import io.iohk.cef.discovery.DiscoveryListener.Ready
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.ServerStatus
import io.iohk.cef.utils.FiniteSizedMap
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Random, Success}
import scala.concurrent.duration._


object DiscoveryManager2 {

  import io.iohk.cef.db.KnownNode
  import io.iohk.cef.network.Node

  sealed trait TypedDiscoveryRequest

  case class TypedStartListening() extends TypedDiscoveryRequest

  case class TypedBlacklist(node: Node) extends TypedDiscoveryRequest

  case class TypedGetDiscoveredNodes(replyTo: ActorRef[DiscoveredNodes]) extends TypedDiscoveryRequest

  case class TypedFetchNeighbors(node: Node) extends TypedDiscoveryRequest

  private[discovery] case class Initialized(address: InetSocketAddress) extends TypedDiscoveryRequest

  private[discovery] case class MessageReceivedWrapper(innerMessage: DiscoveryListener.MessageReceived) extends TypedDiscoveryRequest


  sealed trait DiscoveryResponse

  case class DiscoveredNodes(nodes: Set[KnownNode]) extends DiscoveryResponse

  sealed trait NodeEvent {
    def timestamp: Instant
  }

  case class Sought(node: Node, timestamp: Instant) extends NodeEvent

  case class Pinged(node: Node, timestamp: Instant) extends NodeEvent

  private[discovery] case object Scan

  case object TypedStartListening

  private val nonceSize = 2

  def behaviour(discoveryConfig: DiscoveryConfig,
                knownNodesStorage: KnownNodesStorage,
                nodeState: NodeState,
                clock: Clock,
                encoder: Encoder[DiscoveryWireMessage, ByteString],
                decoder: Decoder[ByteString, DiscoveryWireMessage],
                discoveryListenerProps: untyped.Props,
                randomSource: SecureRandom): Behavior[TypedDiscoveryRequest] = Behaviors.setup {
    context =>

      import akka.actor.typed.scaladsl.adapter._

      val pingedNodes: FiniteSizedMap[ByteString, Pinged] =
        FiniteSizedMap(discoveryConfig.concurrencyDegree, discoveryConfig.messageExpiration * 2, clock)

      val soughtNodes: FiniteSizedMap[ByteString, Sought] =
        FiniteSizedMap(discoveryConfig.concurrencyDegree, discoveryConfig.messageExpiration * 2, clock)

      val buffer = StashBuffer[TypedDiscoveryRequest](capacity = 100)

      val discoveryListener = context.actorOf(discoveryListenerProps)

      def initialBehaviour: Behavior[TypedDiscoveryRequest] = Behaviors.receiveMessage {
        case TypedStartListening() =>
          startListening()
        case msg =>
          buffer.stash(msg)
          Behavior.same
      }

      def startListening(): Behavior[TypedDiscoveryRequest] = Behaviors.setup {
        context =>
          implicit val timeout: Timeout = 1 second // TODO discoveryConfig
          implicit val ec: ExecutionContext = context.executionContext

          ask(discoveryListener, DiscoveryListener.Start).onComplete {
            case Success(Ready(address)) => context.self ! Initialized(address)
            case Failure(cause) => throw cause // TODO
            case Success(_) => ??? // TODO an illegal state
          }

          Behaviors.receiveMessage {
            case Initialized(address) =>
              context.log.debug(
                s"UDP address ${discoveryConfig.port} bound successfully. " +
                s"Pinging ${discoveryConfig.bootstrapNodes.size} bootstrap Nodes.")

              discoveryConfig.bootstrapNodes.foreach(node =>
                sendPing(discoveryListener, address, node)
              )

              // processess all stashed messages with listening
              // and also returns listening as the current behaviour
              buffer.unstashAll(context, listening(address))
            case msg =>
              buffer.stash(msg)
              Behaviors.same
          }
      }

      def listening(address: InetSocketAddress): Behavior[TypedDiscoveryRequest] = Behaviors.receiveMessage {

        case TypedBlacklist(node: Node) =>
          knownNodesStorage.blacklist(node)
          Behavior.same

        case TypedGetDiscoveredNodes(replyTo) =>
          replyTo ! DiscoveredNodes(knownNodesStorage.getAll())
          Behavior.same

        case TypedFetchNeighbors(node: Node) =>
          sendPing(discoveryListener, address, node)
          Behavior.same

        case MessageReceivedWrapper(innerMessage: DiscoveryListener.MessageReceived) =>
          context.log.debug(s"Got a wrapped message $innerMessage")
          processDiscoveryMessage(address, innerMessage)
          Behavior.same

        case TypedStartListening() => ??? // TODO raise an error

        case x => throw new IllegalStateException(s"Unexpected message $x.")
      }

      def sendPing(listener: untyped.ActorRef, listeningAddress: InetSocketAddress, node: Node): Unit = {
        context.log.debug(s"Sending ping to ${node.toUri}")
        val nonce = new Array[Byte](nonceSize)
        randomSource.nextBytes(nonce)
        val ping = Ping(DiscoveryWireMessage.ProtocolVersion, getNode(listeningAddress), expirationTimestamp, ByteString(nonce))
        val key = calculateMessageKey(encoder, ping)
        context.log.debug(s"Ping message: ${ping}")
        context.log.debug(s"In sendPing. Key = ${Hex.encode(key.toArray)}")
        val putResult = pingedNodes.put(key, Pinged(node, clock.instant()))
        context.log.debug(s"In sendPing. putResult = ${putResult}")
        putResult.foreach(pinged => {
          listener ! DiscoveryListener.SendMessage(ping, pinged.node.discoveryAddress)
        })
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

      initialBehaviour
  }

  def calculateMessageKey(encoder: Encoder[DiscoveryWireMessage, ByteString], message: DiscoveryWireMessage): ByteString = {
    val encodedPing = encoder.encode(message)
    crypto.kec256(encodedPing)
  }
}
