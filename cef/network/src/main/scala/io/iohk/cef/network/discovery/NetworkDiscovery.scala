package io.iohk.cef.network.discovery
import io.iohk.cef.network.{NodeId, PeerInfo}

private[network] trait NetworkDiscovery {

  /**
    * Enable message forwarding similar to IP packet forwarding.
    *
    * @param nodeId the destination nodeId.
    * @return According to the routing protocol and nodeId provided,
    *         this can either be
    *         a) the peer info of the node itself or
    *         b) the info of a peer 'near to' the nodeId using some distance metric,
    *            enabling the next hop for message propagation.
    */
  def peer(nodeId: NodeId): Option[PeerInfo]
}
