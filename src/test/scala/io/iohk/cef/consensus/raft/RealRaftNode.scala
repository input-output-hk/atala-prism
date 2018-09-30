package io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.node.RaftNode
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

trait RealRaftNode[Command] {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val timeout = Span(5, Seconds)
  val span = Span(50, Millis)

  implicit val futuresPatienceConfig: ScalaFutures.PatienceConfig =
    ScalaFutures.PatienceConfig(timeout = timeout, interval = span)

  val testNodes = mutable.Map[String, TestNode]()

  def clusterIds: Seq[String]

  def machineCallback: Command => Unit

  class TestNode(nodeId: String, state: PersistentStorage[Command]) {

    val machine: Command => Unit = machineCallback

    def localRpcFactory: RPCFactory[Command] = (nodeId, _, _, _) => {
      new LocalRPC[Command](testNodes(nodeId).raftNode)
    }
    import scala.concurrent.duration._
    val neverTimeout = (100 days, 200 days)

    lazy val raftNode: RaftNode[Command] =
      new RaftNode[Command](nodeId, clusterIds, localRpcFactory, neverTimeout, neverTimeout, machine, state)

    testNodes.put(nodeId, this)
  }

  def anIntegratedCluster(nodeData: Seq[(PersistentStorage[Command], String)]): Seq[TestNode] = {
    val nodes = nodeData.map { case (storage, id) => new TestNode(id, storage) }
    nodes.foreach(_.raftNode)
    nodes
  }
}
