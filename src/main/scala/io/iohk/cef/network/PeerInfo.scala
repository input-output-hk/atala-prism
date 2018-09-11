package io.iohk.cef.network

case class PeerInfo(nodeId: NodeId, configuration: NetworkConfiguration, capabilities: Capabilities = Capabilities(0))
