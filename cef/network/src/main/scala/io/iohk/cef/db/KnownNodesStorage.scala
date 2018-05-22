package io.iohk.cef.db

import java.time.Clock

import akka.util.ByteString
import io.iohk.cef.network.Node

//Implementation only for demo-ing purposes
class KnownNodesStorage(clock: Clock) {

  var nodeMap: Map[ByteString, KnownNode] = Map.empty

  def getNodes(): Set[KnownNode] = nodeMap.values.toSet

  /**
    * Inserts a new node or updates the lastSeen timestamp
    * @param node
    * @return
    */
  def insertNode(node: Node): Unit = {
    val updatedNode = nodeMap.get(node.id).map(_.copy(node = node, lastSeen = clock.instant()))
    nodeMap = nodeMap + ((node.id, updatedNode.getOrElse(KnownNode(node, clock.instant(), clock.instant()))))
  }

  def removeNode(node: Node): Unit = {
    nodeMap = nodeMap - node.id
  }

  //Implementation only for demo-ing purposes
  def blacklist(node: Node): Unit = removeNode(node)

}
