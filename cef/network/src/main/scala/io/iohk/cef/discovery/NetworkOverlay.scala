package io.iohk.cef.discovery

import io.iohk.cef.db.KnownNode
import io.iohk.cef.network.NodeInfo

/**
  * The basic responsibility of an overlay is to map from a logical nodeId to the concrete
  * transport information and capabilities advertised by another node.
  *
  * Thought experiment 1: can we implement ring and mesh topos over either UDP or TCP transports.
  * Thought experiment 2: given a ring overlay, could we then use that to implement a kademlia DHT?
  */
trait NetworkOverlay {
  def discoveredNodes: Set[KnownNode]
  def blacklist(node: NodeInfo): Unit
}
