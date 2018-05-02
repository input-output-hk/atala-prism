package io.iohk.cef.db

import io.iohk.cef.network.Node

class KnownNodesStorage {

  def getNodes(): Set[KnownNode] = ???

  /**
    * Inserts a new node or updates the lastSeen timestamp
    * @param node
    * @return
    */
  def insertNode(node: Node): Unit = ???

  def removeNode(node: Node): Unit = ???

}
