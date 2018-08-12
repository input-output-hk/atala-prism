package io.iohk.cef.network.discovery
import io.iohk.cef.network.{NodeId, PeerInfo}

/**
  * Implements a mesh overlay that selects peers within a
  * given distance to add to a routing table.
  * This table is then allows an implementation of the peer lookup
  * that will eventually converge to the destination.
  */
class MeshNetDiscovery(
    /**
      * This is the nodes own peerInfo
      */
    peerInfo: PeerInfo)
    extends NetworkDiscovery {

  override def peer(nodeId: NodeId): Option[PeerInfo] = ???
}

object MeshNetDiscovery {

  case class FindNodes(nodeId: NodeId)
}
