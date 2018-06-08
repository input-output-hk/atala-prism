package io.iohk.cef.db

import io.iohk.cef.network.Node

import scala.concurrent.duration.FiniteDuration

trait KnownNodeStorage {
  def getAll(): Set[KnownNode]

  /**
    * Inserts a new node or updates the lastSeen timestamp
    * @param node
    * @return
    */
  def insert(node: Node): Long

  def remove(node: Node): Unit

  def blacklist(node: Node, duration: FiniteDuration): Unit
}
