package io.iohk.cef.db

import java.time.Instant

import io.iohk.cef.network.{Node, Endpoint}

//Implementation only for demo-ing purposes
class KnownNodesStorage {

  var nodeMap: Map[Endpoint, KnownNode] = Map.empty

  def getNodes(): Set[KnownNode] = nodeMap.values.toSet

  /**
    * Inserts a new node or updates the lastSeen timestamp
    * @param node
    * @return
    */
  def insertNode(node: Node): Unit = {
    val updatedNode = nodeMap.get(node.endpoint).map(_.copy(node = node, lastSeen = Instant.now()))
    nodeMap = nodeMap + ((node.endpoint, updatedNode.getOrElse(KnownNode(node, Instant.now(), Instant.now()))))
  }

  def removeNode(node: Node): Unit = {
    nodeMap = nodeMap - node.endpoint
  }

}
