package io.iohk.cef.discovery.db

import io.iohk.cef.network.NodeInfo

import scala.concurrent.duration.FiniteDuration

trait KnownNodeStorage {
  def getAll(): Set[KnownNode]

  /**
    * Inserts a new node or updates the lastSeen timestamp
    * @param nodeInfo
    * @return
    */
  def insert(nodeInfo: NodeInfo): Long

  def remove(nodeInfo: NodeInfo): Unit

  def blacklist(nodeInfo: NodeInfo, duration: FiniteDuration): Unit
}
