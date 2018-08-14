package io.iohk.cef.network

case class PeerInfo(nodeId: NodeId,
                    configuration: ConversationalNetworkConfiguration,
                    capabilities: Capabilities = Capabilities(0))
