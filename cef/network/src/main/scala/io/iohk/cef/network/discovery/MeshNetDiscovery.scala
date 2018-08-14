package io.iohk.cef.network.discovery
import io.iohk.cef.network.{NodeId, PeerInfo}

/**
  * Implements a mesh overlay that selects peers within a
  * given distance to add to a routing table.
  * This table is then allows an implementation of the peer lookup
  * that will eventually converge to the destination.
  *
  * @param peerInfo This is the nodes own peerInfo
  * @param bootstrapPeerInfo This is the peerInfo of a bootstrap node.
  */
class MeshNetDiscovery(peerInfo: PeerInfo, bootstrapPeerInfo: PeerInfo) extends NetworkDiscovery {

  override def peer(nodeId: NodeId): Option[PeerInfo] = ???
}

object MeshNetDiscovery {

  case class FindNodes(nodeId: NodeId)
}
