package io.iohk.cef.network.discovery
import io.iohk.cef.network.{NodeId, PeerInfo}

private[network] trait NetworkDiscovery {

  def peer(nodeId: NodeId): Option[PeerInfo]
}
