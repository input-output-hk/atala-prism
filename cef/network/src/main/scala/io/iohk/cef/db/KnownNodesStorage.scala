package io.iohk.cef.db

import java.time.Instant

import io.iohk.cef.network.{Node, NodeAddress}

//Implementation only for demo-ing purposes
class KnownNodesStorage {

  var nodeMap: Map[NodeAddress, KnownNode] = Map.empty

  def getNodes(): Set[KnownNode] = nodeMap.values.toSet

  /**
    * Inserts a new node or updates the lastSeen timestamp
    * @param node
    * @return
    */
  def insertNode(node: Node): Unit = {
    val updatedNode = nodeMap.get(node.address).map(_.copy(node = node, lastSeen = Instant.now()))
    nodeMap = nodeMap + ((node.address, updatedNode.getOrElse(KnownNode(node, Instant.now(), Instant.now()))))
  }

  def removeNode(node: Node): Unit = {
    nodeMap = nodeMap - node.address
  }

}
