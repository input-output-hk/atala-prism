package io.iohk.cef.discovery

import java.net.InetSocketAddress
import java.time.{Clock, Instant}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.agent.Agent
import akka.util.ByteString
import io.iohk.cef.crypto
import io.iohk.cef.db.{KnownNode, KnownNodesStorage}
import io.iohk.cef.discovery.DiscoveryMessage.{Ping, Pong, Seek}
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.{Endpoint, Node}
import io.iohk.cef.utils.{NodeStatus, ServerStatus}

import scala.collection.mutable
import scala.util.Random

class DiscoveryManager(
                      discoveryConfig: DiscoveryConfig,
                      knownNodesStorage: KnownNodesStorage,
                      nodeStatusHolder: Agent[NodeStatus],
                      clock: Clock,
                      encoder: Encoder[DiscoveryMessage, ByteString],
                      decoder: Decoder[ByteString, DiscoveryMessage]) extends Actor with ActorLogging {

  import DiscoveryManager._

  val expirationTimeSec = discoveryConfig.messageExpiration.toSeconds

  val pingedNodes: mutable.LinkedHashMap[ByteString, PingInfo] = mutable.LinkedHashMap.empty

  var discoveredNodes: Vector[DiscoveryNodeInfo] = {
    val bootStrapNodesInfo = discoveryConfig.bootstrapNodes.map(DiscoveryNodeInfo.fromNode(_, clock))
    val knownNodes =
      if (discoveryConfig.discoveryEnabled) knownNodesStorage.getNodes()
      else Set.empty
    knownNodes.map(knownNode => DiscoveryNodeInfo.fromKnownNode(knownNode)).toVector ++ bootStrapNodesInfo
  }

  if (discoveryConfig.discoveryEnabled) startListening()

  def startListening() = {
    val listener = context.actorOf(DiscoveryListener.props(discoveryConfig, nodeStatusHolder, encoder, decoder))
    listener ! DiscoveryListener.Start
    context.become(waitingUdpConnection(listener))
  }

  def listening(listener: ActorRef, address: InetSocketAddress): Receive = {
    case DiscoveryListener.MessageReceived(ping @ Ping(protocolVersion, pingFrom, timestamp), from) =>
        if (hasNotExpired(timestamp) &&
            protocolVersion == DiscoveryMessage.ProtocolVersion &&
            pingFrom.toUdpAddress == from) {
          val messageKey = pingMessageKey(ping)
          val pong = Pong(nodeStatusHolder().capabilities, messageKey, expirationTimestamp)
          listener ! DiscoveryListener.SendMessage(pong, from)
        } else {
          log.warning("Received an invalid Ping message")
        }
    case DiscoveryListener.MessageReceived(Pong(capabilities, token, timestamp), from) =>
      if (hasNotExpired(timestamp)) {
        pingedNodes.get(token).foreach { pingInfo =>
          if (pingInfo.nodeinfo.node.udpSocketAddress == from) {
            pingedNodes -= token
            val node = pingInfo.nodeinfo.node
            if (capabilities.satisfies(nodeStatusHolder().capabilities)) {
              context.system.eventStream.publish(CompatibleNodeFound(node))
              knownNodesStorage.insertNode(node)
              discoveredNodes = discoveredNodes :+ DiscoveryNodeInfo(node, clock.instant())
            }
            val seek = Seek(nodeStatusHolder().capabilities, discoveryConfig.maxSeekResults)
            listener ! DiscoveryListener.SendMessage(seek, node.udpSocketAddress)
          } else {
            log.warning(s"Expected pong message from ${pingInfo.nodeinfo.node} but got it from ${from}")
          }
        }
      } else {
        log.warning("Received an invalid Pong message")
      }

    case DiscoveryListener.MessageReceived(DiscoveryMessage.Seek(capabilities, maxResults), from) =>
      val nodes = discoveredNodes.filter(_.node.capabilities.satisfies(capabilities)).map(_.node)
      val randomNodeSubset = Random.shuffle(nodes).take(maxResults)
      val neighbors = DiscoveryMessage.Neighbors(capabilities, nodes.size, randomNodeSubset)
      listener ! DiscoveryListener.SendMessage(neighbors, from)

    case DiscoveryMessage.Neighbors(capabilities, _, neighbors) if (capabilities.satisfies(nodeStatusHolder().capabilities)) =>

    case Scan =>
      scan(listener, address)
  }

  def hasNotExpired(timestamp: Long) = timestamp < clock.instant().getEpochSecond

  def scan(listener: ActorRef, address: InetSocketAddress): Unit = {
    val now = clock.instant()
    val expired = pingedNodes.takeWhile {
      case (_, nodeInfo) =>
        nodeInfo.addTimestamp
          //the expiration time of a ping + expiration time of a pong. Hence times 2.
          .plusMillis(discoveryConfig.messageExpiration.toMillis * 2)
          .isBefore(now)
    }
    //Eliminating the nodes that never answered.
    //What would be the rules for eliminating them from persistent storage?
    expired.foreach {
      case (id, pingInfo) => {
        pingedNodes -= id
        discoveredNodes = discoveredNodes.filter(_ == pingInfo.nodeinfo)
      }
    }

    new Random().shuffle(pingedNodes.values).take(discoveryConfig.scanMaxNodes).foreach {pingInfo =>
      sendPing(listener, address, pingInfo.nodeinfo)
    }

    discoveredNodes
      .sortBy(_.addTimestamp)
      .takeRight(discoveryConfig.scanMaxNodes)
      .foreach { nodeInfo =>
        sendPing(listener, address, nodeInfo)
      }
  }

  override def receive: Receive = {
    case DiscoveryManager.StartListening => startListening()
    case _ => log.warning("Discovery manager not listening.")
  }

  def waitingUdpConnection(listener: ActorRef): Receive = {
    case DiscoveryListener.Ready(address) =>
      context.become(listening(listener, address))
    case _ =>
      log.warning("UDP connection not ready yet. Ignoring the message.")
  }

  private def sendPing(listener: ActorRef, listeningAddress: InetSocketAddress, nodeInfo: DiscoveryNodeInfo): Unit = {
    val from = Endpoint.fromUdpAddress(listeningAddress, getTcpPort)
    val ping = DiscoveryMessage.Ping(DiscoveryMessage.ProtocolVersion, from, expirationTimestamp)
    val key = pingMessageKey(ping)

    if(pingedNodes.size >= discoveryConfig.nodesLimit) pingedNodes -= pingedNodes.head._1

    pingedNodes += ((key, PingInfo(nodeInfo, clock.instant())))

    listener ! DiscoveryListener.SendMessage(ping, nodeInfo.node.udpSocketAddress)
  }

  private def pingMessageKey(ping: DiscoveryMessage.Ping) = {
    val encodedPing = encoder.encode(ping)
    crypto.kec256(encodedPing)
  }

  private def getTcpPort: Int = nodeStatusHolder().serverStatus match {
    case ServerStatus.Listening(addr) => addr.getPort
    case _ => 0
  }

  private def updateNodes[V <: TimedInfo](map: Map[ByteString, V], key: ByteString, info: V): Map[ByteString, V] = {
    if (map.size < discoveryConfig.nodesLimit) {
      map + (key -> info)
    } else {
      replaceOldestNode(map, key, info)
    }
  }

  private def replaceOldestNode[V <: TimedInfo](map: Map[ByteString, V], key: ByteString, info: V): Map[ByteString, V] = {
    val (earliestNode, _) = map.minBy { case (_, node) => node.addTimestamp }
    val newMap = map - earliestNode
    newMap + (key -> info)
  }

  private def expirationTimestamp = clock.instant().plusSeconds(expirationTimeSec).getEpochSecond
}

object DiscoveryManager {
  def props(discoveryListener: ActorRef,
            discoveryConfig: DiscoveryConfig,
            knownNodesStorage: KnownNodesStorage,
            nodeStatusHolder: Agent[NodeStatus],
            clock: Clock): Props =
    Props(new DiscoveryManager(discoveryListener, discoveryConfig, knownNodesStorage, nodeStatusHolder, clock))

  object DiscoveryNodeInfo {

    def fromKnownNode(knownNode: KnownNode): DiscoveryNodeInfo = DiscoveryNodeInfo(knownNode.node, knownNode.discovered)

    def fromNode(node: Node, clock: Clock): DiscoveryNodeInfo = DiscoveryNodeInfo(node, clock.instant())

  }


  sealed abstract class TimedInfo {
    def addTimestamp: Instant
  }
  case class DiscoveryNodeInfo(node: Node, addTimestamp: Instant) extends TimedInfo
  case class PingInfo(nodeinfo: DiscoveryNodeInfo, addTimestamp: Instant) extends TimedInfo

  case object GetDiscoveredNodesInfo
  case class DiscoveredNodesInfo(nodes: Set[DiscoveryNodeInfo])

  private[discovery] case object Scan

  case object StartListening

  case object Scan

}
