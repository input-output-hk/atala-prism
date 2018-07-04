package io.iohk.cef.discovery

import io.iohk.cef.network.Node

sealed abstract class DiscoveryClassifier

case class CompatibleNodeFound(node: Node) extends DiscoveryClassifier

case class NodeRemoved(node: Node) extends DiscoveryClassifier
