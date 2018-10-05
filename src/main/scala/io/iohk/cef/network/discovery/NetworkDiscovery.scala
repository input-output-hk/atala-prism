package io.iohk.cef.network.discovery
import io.iohk.cef.network.{NodeId, PeerInfo}

trait NetworkDiscovery {

  /**
    * Enable message forwarding similar to IP packet forwarding.
    *
    * @param nodeId the destination nodeId.
    * @return According to the routing protocol and nodeId provided,
    *         this can either be
    *         a) the peer info of the node itself if it is know or
    *         b) the info of a peer 'nearest to' the nodeId using some distance metric.
    *            This enables the next hop for message propagation.
    */
  def nearestPeerTo(nodeId: NodeId): Option[PeerInfo] = nearestNPeersTo(nodeId, 1).headOption

  /**
    * Enable structured gossip by selecting the nearest N known peers to the given node id.
    * Results should be returned sorted ascending in the distance metric.
    */
  def nearestNPeersTo(nodeId: NodeId, n: Int): Seq[PeerInfo]

  /**
    * Stop the discovery instance and clean up.
    */
  def shutdown(): Unit
}
