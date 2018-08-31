package io.iohk.cef.consensus.raft
import akka.dispatch.ExecutionContexts
import io.iohk.cef.consensus.raft.RaftConsensus.{EntriesToAppend, LogEntry, RaftContext}
import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext
import org.scalatest.mockito.MockitoSugar._
//import org.mockito.Mockito.{when}
//import org.scalatest.Matchers._

class RaftContextSpec extends FlatSpec {

  implicit val ec: ExecutionContext = ExecutionContexts.global()

  /*
   rc = RaftContext(io.iohk.cef.consensus.raft.Follower@4f0ee6f7,
   CommonVolatileState(-1,-1),
   LeaderVolatileState(List(0, 0),List(-1, -1)),
   (2,),
   Vector(),
   0,
   Vector(LogEntry(A,2,0), LogEntry(B,2,1), LogEntry(C,2,2), LogEntry(D,2,3), LogEntry(E,2,4)),
   i1)

   rc2 = RaftContext(io.iohk.cef.consensus.raft.Follower@4f0ee6f7,
   CommonVolatileState(-1,-1),
   LeaderVolatileState(List(0, 0),List(-1, -1)),
   (2,),
   Vector(),
   0,
   List(),
   i1)
    */

  it should "copy writes correctly" in {
    val raftNode = mock[RaftNode[String]]

//    when(raftNode.log())
    val role = new Follower[String](raftNode)

    val rc = RaftContext[String](role, CommonVolatileState(0, 0), LeaderVolatileState(Seq(), Seq()), (0, ""),
      Vector(LogEntry("A", 1, 0), LogEntry("B", 1, 1)), 0, Seq(), "leaderId")

    val entries = EntriesToAppend[String](2,"leader",-1,-1,List(),4)
    val tuple = role.appendEntries(rc, entries)
//    println(rc.log)
//
//    println(rc.copy(leaderId = "foo", writes = List()).log)
  }
}
