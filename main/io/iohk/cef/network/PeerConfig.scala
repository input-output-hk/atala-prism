package io.iohk.cef.network

case class PeerConfig(nodeId: NodeId, transportConfig: TransportConfig, capabilities: Capabilities = Capabilities(0))
