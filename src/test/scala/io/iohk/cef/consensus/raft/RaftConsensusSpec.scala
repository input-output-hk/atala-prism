package io.iohk.cef.consensus.raft

import akka.actor.ActorSystem
import akka.dispatch.ExecutionContexts
import io.iohk.cef.consensus.raft.RaftConsensus._
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.mockito.MockitoSugar._

import scala.concurrent.{ExecutionContext, Future}

class RaftConsensusSpec extends WordSpec {

  implicit val ec: ExecutionContext = ExecutionContexts.global

  class InMemoryPersistentStorage[T](var logEntries: Vector[LogEntry[T]], var currentTerm: Long, var votedFor: String)
      extends PersistentStorage[T] {

    override def state: Future[(Long, String)] = Future(currentTerm -> votedFor)

    override def state(currentTerm: Long, votedFor: String): Future[Unit] = {
      this.currentTerm = currentTerm
      this.votedFor = votedFor
      Future(Unit)
    }
    override def log(entry: LogEntry[T]): Future[Unit] = {
      logEntries = logEntries :+ entry
      Future(Unit)
    }
    override def dropRight(n: Int): Future[Unit] = {
      logEntries = logEntries.dropRight(n)
      Future(Unit)
    }
    override def log: Future[Vector[LogEntry[T]]] = Future(logEntries)
  }

  class LocalRPC[T](peer: RaftNode[T]) extends RPC[T] {
    override def appendEntries(entriesToAppend: EntriesToAppend[T]): Future[AppendEntriesResult] =
      peer.appendEntries(entriesToAppend)

    override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] = peer.requestVote(voteRequested)
  }

  private implicit val actorSystem = ActorSystem()

  trait TestRPC {
    type Command = String
    val stateMachine: Command => Unit = mock[Command => Unit]
    val peer = mock[RaftNode[Command]]
    val rpc = new LocalRPC(peer)
    var appendCallback: EntriesToAppend[Command] => Future[AppendEntriesResult] = _
    var voteCallback: VoteRequested => Future[RequestVoteResult] = _
    val rpcFactory: RPCFactory[Command] = (node, appendEntriesCallback, requestVoteCallback) => {
      appendCallback = appendEntriesCallback
      voteCallback = requestVoteCallback
      rpc
    }
  }

  "A follower" when {
    "implementing the Append Entries RPC" should {

      "implement rule #1 - reply false if term < currentTerm)" in new TestRPC {

        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

        val raftNode =
          new RaftNode[Command]("i1", Set("i1", "i2"), rpcFactory, stateMachine, persistentStorage)

        val appendResult = appendCallback(EntriesToAppend(term = 0, leaderId = "anyone", 0, 0, List(), 1))

        appendResult.futureValue shouldBe AppendEntriesResult(term = 1, success = false)
      }

      "implement rule #2 - reply false if log doesn't contain an entry at prevLogIndex" in new TestRPC {

        val log = Vector(LogEntry("A", 1, 1), LogEntry("B", 1, 2), LogEntry("C", 2, 3))

        val persistentStorage =
          new InMemoryPersistentStorage[String](log, currentTerm = 1, votedFor = "anyone")

        val raftNode =
          new RaftNode[Command]("i1", Set("i1", "i2"), rpcFactory, stateMachine, persistentStorage)

        val appendResult = appendCallback(
          EntriesToAppend(term = 1,
                          leaderId = "anyone",
                          prevLogIndex = 4,
                          prevLogTerm = 1,
                          entries = List(),
                          leaderCommitIndex = 1))

        appendResult.futureValue shouldBe AppendEntriesResult(term = 1, success = false)
      }

      "implement rule #3 - if an existing entry conflicts with a new one (same index but different term), " +
      "delete the existing entry and those that follow it" in new TestRPC {

        val log = Vector(LogEntry("A", 1, 1), LogEntry("B", term = 1, 2), LogEntry("C", term = 1, 3))

        val persistentStorage =
          new InMemoryPersistentStorage[String](log, currentTerm = 1, votedFor = "anyone")

        val raftNode =
          new RaftNode[Command]("i1", Set("i1", "i2"), rpcFactory, stateMachine, persistentStorage)

        val appendResult = appendCallback(
          EntriesToAppend(term = 1,
                          leaderId = "anyone",
                          prevLogIndex = 1,
                          prevLogTerm = 1,
                          entries = List(LogEntry("B", term = 2, 2), LogEntry("C", term = 2, 3)),
                          leaderCommitIndex = 4))

        appendResult.futureValue shouldBe AppendEntriesResult(term = 1, success = false)
        persistentStorage.logEntries shouldBe Vector(LogEntry("A", 1, 1))
      }
    }
  }

//  "Rules for servers" when {
//    "All servers" when {
//      "commitIndex > lastApplied" should {
//        "increment last applied" in {}
//        "apply log[lastApplied] to the state machine" in {}
//      }
//    }
//    "Followers" when {
//      "receiving an appendEntries RPC" should {
//        "respond" in {}
//      }
//      "receiving a requestVote RPC" should {
//        "respond" in {}
//      }
//      "election timeout elapses without appendEntries" should {
//        "become a candidate" in {}
//      }
//    }
//  }
}
