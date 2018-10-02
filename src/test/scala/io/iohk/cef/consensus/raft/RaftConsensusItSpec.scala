package io.iohk.cef.consensus.raft

import io.iohk.cef.consensus.raft.node._
import org.mockito.Mockito.{times, verify}
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.mockito.MockitoSugar

class RaftConsensusItSpec extends WordSpec with MockitoSugar {

  "In an integrated cluster" when {
    "servers are started" should {
      "elect a leader" in new RealRaftNodeFixture[String] {
        override def machineCallback: String => Unit = _ => ()
        override def clusterIds: Seq[String] = Seq("i1", "i2", "i3")
        val storages = clusterIds.map(_ => new InMemoryPersistentStorage[String](Vector(), 1, ""))

        val Seq(t1, t2, t3) = anIntegratedCluster(storages.zip(clusterIds))
        t1.raftNode.electionTimeout().futureValue
        t1.raftNode.getRole shouldBe Leader
      }
      "replicate logs" in new RealRaftNodeFixture[String] {
        override def machineCallback: String => Unit = mock[String => Unit]
        override def clusterIds: Seq[String] = Seq("i1", "i2", "i3")
        val storages = clusterIds.map(_ => new InMemoryPersistentStorage[String](Vector(), 1, ""))
        val Seq(s1, s2, s3) = storages

        val Seq(t1, t2, t3) = anIntegratedCluster(storages.zip(clusterIds))

        val consensus = new RaftConsensus(t1.raftNode)

        t1.raftNode.electionTimeout().futureValue

        t1.raftNode.getRole shouldBe Leader

        val appendResult = consensus.appendEntries(Seq("A", "B", "C", "D", "E")).futureValue

        val expectedEntries = Vector(
          LogEntry("A", 2, 0),
          LogEntry("B", 2, 1),
          LogEntry("C", 2, 2),
          LogEntry("D", 2, 3),
          LogEntry("E", 2, 4))
        s1.log shouldBe expectedEntries
        s2.log shouldBe expectedEntries
        s3.log shouldBe expectedEntries

        t1.raftNode.getLeaderVolatileState shouldBe LeaderVolatileState(Seq(5, 5), Seq(4, 4))
        t2.raftNode.getLeaderVolatileState shouldBe LeaderVolatileState(Seq(0, 0), Seq(-1, -1))
        t3.raftNode.getLeaderVolatileState shouldBe LeaderVolatileState(Seq(0, 0), Seq(-1, -1))

        t1.raftNode.getCommonVolatileState shouldBe CommonVolatileState(4, 4)
        t2.raftNode.getCommonVolatileState shouldBe CommonVolatileState(-1, -1)
        t3.raftNode.getCommonVolatileState shouldBe CommonVolatileState(-1, -1)

        Seq("A", "B", "C", "D", "E").foreach(command => {
          verify(t1.machine).apply(command)
          verify(t2.machine, times(0)).apply(command)
          verify(t3.machine, times(0)).apply(command)
        })

        t1.raftNode.heartbeatTimeout().futureValue

        Seq("A", "B", "C", "D", "E").foreach(command => {
          verify(t2.machine).apply(command)
          verify(t3.machine).apply(command)
        })

        t2.raftNode.getCommonVolatileState shouldBe CommonVolatileState(4, 4)
        t3.raftNode.getCommonVolatileState shouldBe CommonVolatileState(4, 4)

        consensus.appendEntries(Seq("F", "G", "H")).futureValue
        t1.raftNode.heartbeatTimeout().futureValue

        Seq("F", "G", "H").foreach(command => {
          verify(t1.machine).apply(command)
          verify(t2.machine).apply(command)
          verify(t3.machine).apply(command)
        })

        val expectedEntries2 = Vector(
          LogEntry("A", 2, 0),
          LogEntry("B", 2, 1),
          LogEntry("C", 2, 2),
          LogEntry("D", 2, 3),
          LogEntry("E", 2, 4),
          LogEntry("F", 2, 5),
          LogEntry("G", 2, 6),
          LogEntry("H", 2, 7))

        s1.log shouldBe expectedEntries2
        s2.log shouldBe expectedEntries2
        s3.log shouldBe expectedEntries2
      }
    }
  }
}
