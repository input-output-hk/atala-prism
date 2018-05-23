package io.iohk.cef.discovery

import io.iohk.cef.db.KnownNode
import io.iohk.cef.network.Node

sealed trait DiscoveryRequest

case class Blacklist(node: Node) extends DiscoveryRequest

case class GetDiscoveredNodes() extends DiscoveryRequest

case class FetchNeighbors(node: Node) extends DiscoveryRequest

sealed trait DiscoveryResponse

case class DiscoveredNodes(nodes: Set[KnownNode]) extends DiscoveryResponse
