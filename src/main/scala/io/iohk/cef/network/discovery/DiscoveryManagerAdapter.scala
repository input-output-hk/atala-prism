package io.iohk.cef.network.discovery

import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import akka.{actor => untyped}
import io.iohk.cef.network.discovery.DiscoveryManager.{DiscoveredNodes, DiscoveryRequest, GetDiscoveredNodes}
import io.iohk.cef.network.discovery.db.KnownNode
import io.iohk.cef.network.transport.FrameHeader
import io.iohk.cef.network.transport.tcp.TcpTransportConfiguration
import io.iohk.cef.network.{ConversationalNetworkConfiguration, NodeId, PeerInfo}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class DiscoveryManagerAdapter(discoveryManager: ActorRef[DiscoveryRequest])(implicit system: untyped.ActorSystem)
    extends NetworkDiscovery {

  implicit val futureTimeout: Duration = 1 second
  implicit val askTimeout: Timeout = 1 second
  implicit val scheduler: Scheduler = system.scheduler

  override def peer(nodeId: NodeId): Option[PeerInfo] = {
    val futureResult: Future[DiscoveredNodes] = discoveryManager ? GetDiscoveredNodes

    val discoveredNodes: DiscoveredNodes = Await.result(futureResult, futureTimeout)

    discoveredNodes.nodes
      .find(knownNode => NodeId(knownNode.node.id) == nodeId)
      .map(adaptToPeerInfo)
  }

  private def adaptToPeerInfo(knownNode: KnownNode): PeerInfo = {
    val itsNodeId = NodeId(knownNode.node.id)
    val itsConfiguration =
      ConversationalNetworkConfiguration(Some(TcpTransportConfiguration(knownNode.node.serverAddress)),
        FrameHeader.defaultTtl)
    PeerInfo(itsNodeId, itsConfiguration)
  }
}
