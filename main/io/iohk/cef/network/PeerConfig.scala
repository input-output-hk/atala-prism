package io.iohk.cef.network

case class PeerConfig(nodeId: NodeId, networkConfig: NetworkConfig, capabilities: Capabilities = Capabilities(0))
