package io.iohk.cef.network.discovery

import io.iohk.cef.network.NodeInfo

sealed abstract class DiscoveryClassifier

case class CompatibleNodeFound(nodeInfo: NodeInfo) extends DiscoveryClassifier

case class NodeRemoved(nodeInfo: NodeInfo) extends DiscoveryClassifier
