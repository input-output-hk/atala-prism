package io.iohk.cef.discovery

import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.{Clock, Instant}

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.crypto
import io.iohk.cef.db.KnownNodesStorage
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.{NodeState, NodeStatusMessage}
import io.iohk.cef.network.{Endpoint, Node, NodeStatus, ServerStatus}
import io.iohk.cef.utils.FiniteSizedMap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class DiscoveryManager(
                        discoveryConfig: DiscoveryConfig,
                        knownNodesStorage: KnownNodesStorage,
                        nodeStatusHolder: ActorRef[NodeStatusMessage],
                        clock: Clock,
                        encoder: Encoder[DiscoveryWireMessage, ByteString],
                        decoder: Decoder[ByteString, DiscoveryWireMessage],
                        listenerMaker: untyped.ActorRefFactory => untyped.ActorRef,
                        scheduler: untyped.Scheduler,
                        randomSource: SecureRandom) extends untyped.Actor with untyped.ActorLogging with untyped.Stash {

  import DiscoveryManager._

  val pingedNodes: FiniteSizedMap[ByteString, Pinged] =
    FiniteSizedMap(discoveryConfig.concurrencyDegree, discoveryConfig.messageExpiration * 2, clock)

  val soughtNodes: FiniteSizedMap[ByteString, Sought] =
    FiniteSizedMap(discoveryConfig.concurrencyDegree, discoveryConfig.messageExpiration * 2, clock)

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
      nodeStatusHolder ! NodeStatus.UpdateDiscoveryStatus(ServerStatus.Listening(address))
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
    case DiscoveryListener.MessageReceived(ping @ Ping(protocolVersion, sourceNode, timestamp, _), from) =>
        if (hasNotExpired(timestamp) &&
            protocolVersion == DiscoveryWireMessage.ProtocolVersion) {
          log.debug(s"Received a ping message from ${from}, replyTo: ${sourceNode.discoveryAddress}")
          val messageKey = calculateMessageKey(ping)
          val node = getNode(discoveryAddress = address)
          val pong = Pong(node, messageKey, expirationTimestamp)
          knownNodesStorage.insertNode(sourceNode)
          log.debug(s"Sending pong message with capabilities ${pong.node.capabilities}, to: ${sourceNode.discoveryAddress}")
          listener ! DiscoveryListener.SendMessage(pong, sourceNode.discoveryAddress)
        } else {
          log.warning(s"Received an invalid Ping message")
        }
    case DiscoveryListener.MessageReceived(Pong(pingedNode, token, timestamp), from) =>
      if (hasNotExpired(timestamp)) {
        pingedNodes.get(token).foreach { _ =>
          log.debug(s"Received a pong message from ${from}")
          pingedNodes -= token
          if (pingedNode.capabilities.satisfies(nodeStatus.capabilities)) {
            context.system.eventStream.publish(CompatibleNodeFound(pingedNode))
            knownNodesStorage.insertNode(pingedNode)
            log.debug(s"New discovered list: ${knownNodesStorage.getNodes().map(_.node.discoveryAddress)}")
          } else {
            log.debug(s"Node received in pong ${from} does not satisfy the capabilities.")
          }
          val nonce = new Array[Byte](nonceSize)
          randomSource.nextBytes(nonce)
          val seek = Seek(nodeStatus.capabilities, discoveryConfig.maxSeekResults, expirationTimestamp, ByteString(nonce))
          log.debug(s"Sending seek message ${seek} to ${pingedNode.discoveryAddress}")
          listener ! DiscoveryListener.SendMessage(seek, pingedNode.discoveryAddress)
          val messageKey = calculateMessageKey(seek)
          soughtNodes.put(messageKey, Sought(pingedNode, clock.instant()))
        }
      } else {
        log.warning("Received an invalid Pong message")
      }

    case DiscoveryListener.MessageReceived(seek @ Seek(capabilities, maxResults, timestamp, _), from) =>
      if(hasNotExpired(timestamp)) {
        log.debug(s"Received a seek message ${seek} from ${from}")
        val nodes =
          knownNodesStorage.getNodes().filter(_.node.capabilities.satisfies(capabilities)).map(_.node)
        log.debug(s"Nodes satisfying capabilities = ${nodes.map(_.discoveryAddress)}")
        val randomNodeSubset = Random.shuffle(nodes).take(maxResults)
        val token = calculateMessageKey(seek)
        val neighbors = Neighbors(capabilities, token, nodes.size, randomNodeSubset.toSeq, expirationTimestamp)
        log.debug(s"Sending neighbors answer with ${randomNodeSubset} to ${from}")
        listener ! DiscoveryListener.SendMessage(neighbors, from)
      } else {
        log.warning("Received an invalid Seek message")
      }
    case DiscoveryListener.MessageReceived(Neighbors(capabilities, token, _, neighbors, timestamp), from) =>
      if (hasNotExpired(timestamp) &&
        capabilities.satisfies(nodeStatus.capabilities)) {
        log.debug(s"Received a neighbors message from ${from}. Neighbors: $neighbors")
        val discoveredNodes = knownNodesStorage.getNodes().map(_.node).union(pingedNodes.values.map(_.node).toSet)
        soughtNodes.get(token).foreach { _ =>
          val newNodes = if (discoveryConfig.multipleConnectionsPerAddress) {
            val nodeEndpoints = discoveredNodes.map(dn => (ByteString(dn.discoveryAddress.getAddress.getAddress), dn.discoveryAddress.getPort))
            neighbors.filterNot(node => nodeEndpoints.contains((ByteString(node.discoveryAddress.getAddress.getAddress), node.discoveryAddress.getPort)))
          } else {
            val nodeEndpoints = discoveredNodes.map(dn => ByteString(dn.discoveryAddress.getAddress.getAddress))
            neighbors.filterNot(node => nodeEndpoints.contains(ByteString(node.discoveryAddress.getAddress.getAddress)))
          }
          log.debug(s"Sending ping to ${newNodes.size} nodes. Nodes: ${newNodes.map(_.discoveryAddress)}")
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
    val expired = pingedNodes.dropExpired

    //Eliminating the nodes that never answered.
    expired.foreach {
      case (id, pingInfo) => {
        pingedNodes -= id
        knownNodesStorage.removeNode(pingInfo.node)
      }
    }

    new Random().shuffle(pingedNodes.values).take(discoveryConfig.scanNodesLimit).foreach { pingInfo =>
      sendPing(listener, address, pingInfo.node)
    }

    knownNodesStorage.getNodes().toSeq
      .sortBy(_.lastSeen)
      .takeRight(discoveryConfig.scanNodesLimit)
      .foreach { nodeInfo =>
        sendPing(listener, address, nodeInfo.node)
      }
  }

  private def sendPing(listener: untyped.ActorRef, listeningAddress: InetSocketAddress, node: Node): Unit = {
    log.debug(s"Sending ping to ${node}")
    val nonce = new Array[Byte](nonceSize)
    randomSource.nextBytes(nonce)
    val ping = Ping(DiscoveryWireMessage.ProtocolVersion, getNode(listeningAddress), expirationTimestamp, ByteString(nonce))
    val key = calculateMessageKey(ping)
    log.debug(s"Ping message: ${ping}")

    pingedNodes.put(key, Pinged(node, clock.instant())).foreach(pinged => {
      listener ! DiscoveryListener.SendMessage(ping, pinged.node.discoveryAddress)
    })
  }

  private def processNodeStateUpdated: Receive = {
    case NodeStatus.StateUpdated(state) =>
      log.debug(s"State ${state} received. Proceeding to listening if configured.")
      nodeStatus = state
  }

  private def calculateMessageKey(message: DiscoveryWireMessage) = {
    val encodedPing = encoder.encode(message)
    crypto.kec256(encodedPing)
  }

  private def getServerAddress(default: InetSocketAddress): InetSocketAddress = nodeStatus.serverStatus match {
    case ServerStatus.Listening(addr) => addr
    case _ => default
  }

  private def updateNodes[V <: NodeEvent](map: Map[ByteString, V], key: ByteString, info: V): Map[ByteString, V] = {
    if (map.size < discoveryConfig.discoveredNodesLimit) {
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

  private def equivalentAddresses(nodeAddress: Endpoint, socketAddress: InetSocketAddress) = {
    if(discoveryConfig.multipleConnectionsPerAddress) {
      nodeAddress.equalUdpAddress(socketAddress)
    } else {
      nodeAddress.equalIpAddress(socketAddress.getAddress)
    }
  }

  private def getNode(discoveryAddress: InetSocketAddress): Node =
    Node(nodeStatus.nodeId, discoveryAddress, getServerAddress(default = discoveryAddress), nodeStatus.capabilities)

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
            encoder: Encoder[DiscoveryWireMessage, ByteString],
            decoder: Decoder[ByteString, DiscoveryWireMessage],
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
                    encoder: Encoder[DiscoveryWireMessage, ByteString],
                    decoder: Decoder[ByteString, DiscoveryWireMessage])
                   (actorRefFactory: untyped.ActorRefFactory) = {
    actorRefFactory.actorOf(DiscoveryListener.props(
      discoveryConfig,
      nodeStatusHolder,
      encoder,
      decoder
    ))
  }

  sealed abstract trait NodeEvent {
    def timestamp: Instant
  }
  case class Sought(node: Node, timestamp: Instant) extends NodeEvent
  case class Pinged(node: Node, timestamp: Instant) extends NodeEvent

  private[discovery] case object Scan

  case object StartListening

}
