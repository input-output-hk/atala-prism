package io.iohk.cef.discovery

import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.{Clock, Instant}

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.crypto
import io.iohk.cef.db.{KnownNode, KnownNodesStorage}
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.{NodeState, NodeStatusMessage}
import io.iohk.cef.network.{Capabilities, Endpoint, Node, NodeAddress, NodeStatus, ServerStatus}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class DiscoveryManager(
                        discoveryConfig: DiscoveryConfig,
                        knownNodesStorage: KnownNodesStorage,
                        nodeStatusHolder: ActorRef[NodeStatusMessage],
                        clock: Clock,
                        encoder: Encoder[DiscoveryMessage, ByteString],
                        decoder: Decoder[ByteString, DiscoveryMessage],
                        listenerMaker: untyped.ActorRefFactory => untyped.ActorRef,
                        scheduler: untyped.Scheduler,
                        randomSource: SecureRandom) extends untyped.Actor with untyped.ActorLogging with untyped.Stash {

  import DiscoveryManager._

  val pingedNodes: mutable.LinkedHashMap[ByteString, Pinged] = mutable.LinkedHashMap.empty

  var discoveredNodes: Vector[Discovered] = {
    val knownNodes =
      if (discoveryConfig.discoveryEnabled) knownNodesStorage.getNodes()
      else Set.empty
    knownNodes.map(knownNode => Discovered.fromKnownNode(knownNode)).toVector
  }

  val soughtNodes: mutable.LinkedHashMap[ByteString, Int] = mutable.LinkedHashMap.empty

  var nodeStatus: NodeState = _

  val nonceSize = 2

  nodeStatusHolder ! NodeStatus.Subscribe(self)

  override def receive: Receive = {
    case NodeStatus.StateUpdated(state) =>
      processNodeStateUpdated(NodeStatus.StateUpdated(state))
      unstashAll()
      if(discoveryConfig.discoveryEnabled) startListening()
      else context.become(stateLoaded)
    case m =>
      log.warning(s"NodeState has not loaded yet. Stashing the message of type ${m.getClass}.")
      stash()
  }

  def stateLoaded: Receive = processNodeStateUpdated orElse {
    case DiscoveryManager.StartListening => startListening()
    case _ => log.warning("Discovery manager not listening.")
  }

  def startListening() = {
    val listener = listenerMaker(context)
    listener ! DiscoveryListener.Start
    scheduler.schedule(discoveryConfig.scanInitialDelay, discoveryConfig.scanInterval, self, Scan)
    context.become(waitingUdpConnection(listener))
    log.debug("Waiting for UDP Connection")
  }

  def waitingUdpConnection(listener: untyped.ActorRef): Receive = processNodeStateUpdated orElse {
    case DiscoveryListener.Ready(address) =>
      context.become(listening(listener, address))
      log.debug(s"UDP address ${address} was bound. Pinging ${discoveryConfig.bootstrapNodes.size} Bootstrap Nodes.")

      //Pinging all bootstrap nodes
      discoveryConfig.bootstrapNodes.foreach( node =>
        sendPing(listener, address, node)
      )
    case message =>
      log.warning(s"UDP connection not ready yet. Ignoring the message. Received: ${message}")
  }

  def listening(listener: untyped.ActorRef, address: InetSocketAddress): Receive = processNodeStateUpdated orElse {
    case DiscoveryListener.MessageReceived(ping @ Ping(protocolVersion, replyTo, timestamp, _), from) =>
        if (hasNotExpired(timestamp) &&
            protocolVersion == DiscoveryMessage.ProtocolVersion) {
          log.debug(s"Received a ping message from ${from}, replyTo: $replyTo")
          val messageKey = calculateMessageKey(ping)
          val pong = Pong(nodeStatus.capabilities, messageKey, expirationTimestamp)
          log.debug(s"Sending pong message with capabilities ${pong.capabilities}, to: ${replyTo.toUdpAddress}")
          listener ! DiscoveryListener.SendMessage(pong, replyTo.toUdpAddress)
        } else {
          log.warning(s"Received an invalid Ping message")
        }
    case DiscoveryListener.MessageReceived(Pong(capabilities, token, timestamp), from) =>
      if (hasNotExpired(timestamp)) {
        pingedNodes.get(token).foreach { pingInfo =>
          log.debug(s"Received a pong message from ${from}")
          //TODO should I check the sending address matches with the one stored?
          pingedNodes -= token
          val node = pingInfo.node.copy(capabilities = capabilities)
          if (capabilities.satisfies(nodeStatus.capabilities)) {
            context.system.eventStream.publish(CompatibleNodeFound(node))
            knownNodesStorage.insertNode(node)
            discoveredNodes = discoveredNodes :+ Discovered(node, clock.instant())
            log.debug(s"New discovered list: ${discoveredNodes.map(_.node.address.udpSocketAddress)}")
          } else {
            log.debug(s"Node received in pong ${from} does not satisfy the capabilities.")
          }
          val nonce = new Array[Byte](nonceSize)
          randomSource.nextBytes(nonce)
          val seek = Seek(nodeStatus.capabilities, discoveryConfig.maxSeekResults, expirationTimestamp, nonce)
          log.debug(s"Sending seek message ${seek} to ${node.address.udpSocketAddress}")
          listener ! DiscoveryListener.SendMessage(seek, node.address.udpSocketAddress)
          val messageKey = calculateMessageKey(seek)
          soughtNodes += ((messageKey, discoveryConfig.maxSeekResults))
        }
      } else {
        log.warning("Received an invalid Pong message")
      }

    case DiscoveryListener.MessageReceived(seek @ Seek(capabilities, maxResults, timestamp, _), from) =>
      if(hasNotExpired(timestamp)) {
        log.debug(s"Received a seek message ${seek} from ${from}")
        val nodes =
          discoveredNodes.filter(discovered => {
            discovered.node.capabilities.satisfies(capabilities) &&
              !equivalentAddresses(discovered.node.address, from)
          }).map(_.node)
        log.debug(s"Nodes satisfying capabilities = ${nodes.map(_.address.udpSocketAddress)}")
        val randomNodeSubset = Random.shuffle(nodes).take(maxResults)
        val token = calculateMessageKey(seek)
        val neighbors = Neighbors(capabilities, token, nodes.size, randomNodeSubset, expirationTimestamp)
        log.debug(s"Sending neighbors answer with ${randomNodeSubset} to ${from}")
        listener ! DiscoveryListener.SendMessage(neighbors, from)
      } else {
        log.warning("Received an invalid Seek message")
      }
    case DiscoveryListener.MessageReceived(Neighbors(capabilities, token, _, neighbors, timestamp), from) =>
      if (hasNotExpired(timestamp) &&
        capabilities.satisfies(nodeStatus.capabilities)) {
        log.debug(s"Received a neighbors message from ${from}. Neighbors: $neighbors")
        soughtNodes.get(token).foreach { maxSeekResults =>
          val croppedNeighbors = neighbors.take(maxSeekResults)
          val newNodes = if (discoveryConfig.multipleConnectionsPerAddress) {
            val nodeAddresses = discoveredNodes.map(_.node.address).toSet
            croppedNeighbors.filterNot(node => nodeAddresses.contains(node.address))
          } else {
            val nodeAddresses = discoveredNodes.map(_.node.address.addr.getHostAddress).toSet
            croppedNeighbors.filterNot(node => nodeAddresses.contains(node.address.addr.getHostAddress))
          }
          log.debug(s"Sending ping to ${newNodes.size} nodes. Nodes: ${newNodes.map(_.address.udpSocketAddress)}")
          newNodes.foreach(node => {
            sendPing(listener, address, node)
          })
          soughtNodes -= token
        }
      } else {
        log.warning("Received an invalid Neighbors message")
      }
    case Scan =>
      scan(listener, address)
  }

  def scan(listener: untyped.ActorRef, address: InetSocketAddress): Unit = {
    val now = clock.instant()
    val expired = pingedNodes.takeWhile {
      case (_, nodeInfo) =>
        nodeInfo.timestamp
          //the expiration time of a ping + expiration time of a pong. Hence times 2.
          .plusMillis(discoveryConfig.messageExpiration.toMillis * 2)
          .isBefore(now)
    }
    //Eliminating the nodes that never answered.
    //What would be the rules for eliminating them from persistent storage?
    expired.foreach {
      case (id, pingInfo) => {
        pingedNodes -= id
        discoveredNodes = discoveredNodes.filter(_.node.address == pingInfo.node.address)
      }
    }

    new Random().shuffle(pingedNodes.values).take(discoveryConfig.scanMaxNodes).foreach {pingInfo =>
      sendPing(listener, address, pingInfo.node)
    }

    discoveredNodes
      .sortBy(_.timestamp)
      .takeRight(discoveryConfig.scanMaxNodes)
      .foreach { nodeInfo =>
        sendPing(listener, address, nodeInfo.node)
      }
  }

  private def sendPing(listener: untyped.ActorRef, listeningAddress: InetSocketAddress, node: Node): Unit = {
    log.debug(s"Sending ping to ${node}")
    val from = Endpoint.fromUdpAddress(listeningAddress, getTcpPort)
    val nonce = new Array[Byte](nonceSize)
    randomSource.nextBytes(nonce)
    val ping = Ping(DiscoveryMessage.ProtocolVersion, from, expirationTimestamp, nonce)
    val key = calculateMessageKey(ping)
    log.debug(s"Ping message: ${ping}")

    if(pingedNodes.size >= discoveryConfig.nodesLimit) pingedNodes -= pingedNodes.head._1

    listener ! DiscoveryListener.SendMessage(ping, node.address.udpSocketAddress)

    pingedNodes += ((key, Pinged(node, clock.instant())))
    log.debug(s"""New pinged nodes: ${pingedNodes.map(pair => s"${pair._1.toArray.map("%02x".format(_)).mkString} -> ${pair._2.node.address.udpSocketAddress}" )}""")
  }

  private def processNodeStateUpdated: Receive = {
    case NodeStatus.StateUpdated(state) =>
      log.debug(s"State ${state} received. Proceeding to listening if configured.")
      nodeStatus = state
  }

  private def calculateMessageKey(message: DiscoveryMessage) = {
    val encodedPing = encoder.encode(message)
    crypto.kec256(encodedPing)
  }

  private def getTcpPort: Int = nodeStatus.serverStatus match {
    case ServerStatus.Listening(addr) => addr.getPort
    case _ => 0
  }

  private def updateNodes[V <: NodeEvent](map: Map[ByteString, V], key: ByteString, info: V): Map[ByteString, V] = {
    if (map.size < discoveryConfig.nodesLimit) {
      map + (key -> info)
    } else {
      replaceOldestNode(map, key, info)
    }
  }

  private def replaceOldestNode[V <: NodeEvent](map: Map[ByteString, V], key: ByteString, info: V): Map[ByteString, V] = {
    val (earliestNode, _) = map.minBy { case (_, node) => node.timestamp }
    val newMap = map - earliestNode
    newMap + (key -> info)
  }

  private def equivalentAddresses(nodeAddress: NodeAddress, socketAddress: InetSocketAddress) = {
    if(discoveryConfig.multipleConnectionsPerAddress) {
      nodeAddress.equalUdpAddress(socketAddress)
    } else {
      nodeAddress.equalIpAddress(socketAddress.getAddress)
    }
  }

  private def hasNotExpired(timestamp: Long) =
    timestamp > clock.instant().getEpochSecond

  private def expirationTimestamp =
    clock.instant().plusSeconds(discoveryConfig.messageExpiration.toSeconds).getEpochSecond
}

object DiscoveryManager {
  def props(discoveryConfig: DiscoveryConfig,
            knownNodesStorage: KnownNodesStorage,
            nodeStatusHolder: ActorRef[NodeStatusMessage],
            clock: Clock,
            encoder: Encoder[DiscoveryMessage, ByteString],
            decoder: Decoder[ByteString, DiscoveryMessage],
            listenerMaker: untyped.ActorRefFactory => untyped.ActorRef,
            scheduler: untyped.Scheduler,
            secureRandom: SecureRandom): untyped.Props =
    untyped.Props(new DiscoveryManager(
      discoveryConfig,
      knownNodesStorage,
      nodeStatusHolder,
      clock,
      encoder,
      decoder,
      listenerMaker,
      scheduler,
      secureRandom))

  def listenerMaker(discoveryConfig: DiscoveryConfig,
                    nodeStatusHolder: ActorRef[NodeStatusMessage],
                    encoder: Encoder[DiscoveryMessage, ByteString],
                    decoder: Decoder[ByteString, DiscoveryMessage])
                   (actorRefFactory: untyped.ActorRefFactory) = {
    actorRefFactory.actorOf(DiscoveryListener.props(
      discoveryConfig,
      nodeStatusHolder,
      encoder,
      decoder
    ))
  }

  object Discovered {

    def fromKnownNode(knownNode: KnownNode): Discovered =
      Discovered(knownNode.node, knownNode.lastSeen)

    def fromNode(node: Node, capabilities: Capabilities, clock: Clock): Discovered =
      Discovered(node, clock.instant())

  }

  sealed abstract trait NodeEvent {
    def timestamp: Instant
  }
  case class Discovered(node: Node, timestamp: Instant) extends NodeEvent
  case class Pinged(node: Node, timestamp: Instant) extends NodeEvent

  case object GetDiscoveredNodesInfo
  case class DiscoveredNodesInfo(nodes: Set[Discovered])

  private[discovery] case object Scan

  case object StartListening

}
