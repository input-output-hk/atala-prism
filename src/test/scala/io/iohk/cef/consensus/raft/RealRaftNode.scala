package io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.node.RaftNode
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar.mock
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

  val clusterIds: Seq[String] = Seq("i1", "i2", "i3")

  class TestNode(nodeId: String, state: PersistentStorage[Command]) {

    val machine: Command => Unit = mock[Command => Unit]

    def localRpcFactory: RPCFactory[Command] = (nodeId, _, _, _) => {
      new LocalRPC[Command](testNodes(nodeId).raftNode)
    }
    import scala.concurrent.duration._
    val neverTimeout = (100 days, 200 days)

    lazy val raftNode: RaftNode[Command] =
      new RaftNode[Command](nodeId, clusterIds, localRpcFactory, neverTimeout, neverTimeout, machine, state)

    testNodes.put(nodeId, this)
  }

  def anIntegratedCluster(
      i1State: PersistentStorage[Command],
      i2State: PersistentStorage[Command],
      i3State: PersistentStorage[Command]): (TestNode, TestNode, TestNode) = {

    val (t1, t2, t3) = (new TestNode("i1", i1State), new TestNode("i2", i2State), new TestNode("i3", i3State))
    t1.raftNode
    t2.raftNode
    t3.raftNode
    (t1, t2, t3)
  }
}
