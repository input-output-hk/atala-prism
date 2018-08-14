package io.iohk.cef.network.discovery
import java.net.InetSocketAddress

import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.{NodeId, PeerInfo}

/**
  * Implements a mesh overlay that selects peers within a
  * given distance to add to a routing table.
  * This table is then allows an implementation of the peer lookup
  * that will eventually converge to the destination.
  *
  * @param peerInfo This is the nodes own peerInfo
  * @param bootstrapPeerInfo This is the peerInfo of a bootstrap node.
  * @param transports helpers to obtain network transport instances.
  */
class MeshNetDiscovery(peerInfo: PeerInfo, bootstrapPeerInfo: PeerInfo, transports: Transports)
    extends NetworkDiscovery {


  override def peer(nodeId: NodeId): Option[PeerInfo] = ???
}

object MeshNetDiscovery {

  def findNodesHandler(address: InetSocketAddress, message: FindNodes): Unit = ???

  def pingHandler(address: InetSocketAddress, message: Ping): Unit = ???

  case class Ping(peerInfo: PeerInfo)
  case class FindNodes(nodeId: NodeId)
}
