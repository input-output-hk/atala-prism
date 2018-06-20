package io.iohk.cef.db

import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger

import akka.util.ByteString
import io.iohk.cef.network.Node
import io.iohk.cef.telemetery.DatadogTelemetry

import scala.concurrent.duration.FiniteDuration

//Implementation only for demo-ing purposes
class DummyKnownNodesStorage(clock: Clock) extends KnownNodeStorage with DatadogTelemetry {

  var nodeMap: Map[ByteString, KnownNode] = Map.empty

  val trackingKnownNodes =
    registry.gauge("known_nodes", new AtomicInteger(getAll().size))

  override def getAll(): Set[KnownNode] = nodeMap.values.toSet

  override def insert(node: Node): Long = {
    val updatedNode = nodeMap.get(node.id).map(_.copy(node = node, lastSeen = clock.instant()))
    nodeMap = nodeMap + ((node.id, updatedNode.getOrElse(KnownNode(node, clock.instant(), clock.instant()))))
    if(updatedNode.isEmpty) trackingKnownNodes.incrementAndGet()
    0L
  }

  override def remove(node: Node): Unit = {
    if(nodeMap.contains(node.id)) trackingKnownNodes.decrementAndGet()
    nodeMap = nodeMap - node.id
  }

  //Implementation only for demo-ing purposes
  override def blacklist(node: Node, duration: FiniteDuration): Unit = remove(node)

}