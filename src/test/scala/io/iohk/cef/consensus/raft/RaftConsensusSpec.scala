package io.iohk.cef.consensus.raft

import akka.actor.ActorSystem
import akka.dispatch.ExecutionContexts
import io.iohk.cef.consensus.raft.RaftConsensus._
import org.scalatest.{BeforeAndAfterEach, Suite, WordSpec}
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar._
import org.mockito.Mockito.{inOrder, verify, when, reset}
import org.mockito.ArgumentMatchers.any

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class RaftConsensusSpec extends WordSpec with TestRPC {
  "AppendEntries RPC" when {
    "implemented by a Follower" should {
      "implement rule #1" should {
        "reply false if term < currentTerm)" in {

          val persistentStorage =
            new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

          val _ = aRaftNode(persistentStorage)
          val appendResult = appendCallback(EntriesToAppend(term = 0, leaderId = "anyone", 0, 0, List(), 1))

          appendResult shouldBe AppendEntriesResult(term = 1, success = false)
        }
      }
      "implement rule #2" should {
        "reply false if log doesn't contain an entry at prevLogIndex" in {

          val persistentStorage =
            new InMemoryPersistentStorage[String](Vector(LogEntry("A", 1, 0), LogEntry("B", 1, 1), LogEntry("C", 2, 2)),
                                                  currentTerm = 1,
                                                  votedFor = "anyone")

          val _ = aRaftNode(persistentStorage)

          val appendResult = appendCallback(
            EntriesToAppend(term = 1,
                            leaderId = "anyone",
                            prevLogIndex = 3,
                            prevLogTerm = 1,
                            entries = List(),
                            leaderCommitIndex = 1))

          appendResult shouldBe AppendEntriesResult(term = 1, success = false)
        }
      }
      "implement rule #3" should {

        "if an existing entry conflicts with a new one (same index but different term), " +
          "delete the existing entry and those that follow it" in {

          val persistentStorage =
            new InMemoryPersistentStorage[String](Vector(LogEntry("A", 1, 0),
                                                         LogEntry("B", term = 1, 1),
                                                         LogEntry("C", term = 1, 2)),
                                                  currentTerm = 1,
                                                  votedFor = "anyone")

          val raftNode = aRaftNode(persistentStorage)

          val appendResult = appendCallback(
            EntriesToAppend(term = 1,
                            leaderId = "anyone",
                            prevLogIndex = 1,
                            prevLogTerm = 1,
                            entries = List(LogEntry("B", term = 2, 1), LogEntry("C", term = 2, 2)),
                            leaderCommitIndex = 4))

          appendResult shouldBe AppendEntriesResult(term = 1, success = false)
          raftNode.getLog shouldBe Vector(LogEntry("A", 1, 0))
        }
      }
      "implement rule #4" should {
        "append new entries not already in the log" in {

          val persistentStorage =
            new InMemoryPersistentStorage[String](Vector(LogEntry("A", 1, index = 0),
                                                         LogEntry("B", 1, index = 1),
                                                         LogEntry("C", 1, index = 2)),
                                                  currentTerm = 1,
                                                  votedFor = "anyone")

          val raftNode = aRaftNode(persistentStorage)

          val appendResult = appendCallback(
            EntriesToAppend(term = 1,
                            leaderId = "anyone",
                            prevLogIndex = 1,
                            prevLogTerm = 1,
                            entries = List(LogEntry("C", 1, index = 2), LogEntry("D", 1, index = 3)),
                            leaderCommitIndex = 3))

          appendResult shouldBe AppendEntriesResult(term = 1, success = true)

          raftNode.getLog shouldBe Vector(LogEntry("A", 1, index = 0),
                                                       LogEntry("B", 1, index = 1),
                                                       LogEntry("C", 1, index = 2),
                                                       LogEntry("D", 1, index = 3))
        }
      }
      "implement rule #5" should {
        "if leaderCommit > commitIndex set commitIndex = min(leaderCommit, index of last new entry)" in {
          // by 'Rules for servers', note for all servers,
          // if commitIndex > lastApplied, apply log[lastApplied] to state machine,
          // and
          // by 'state', 'volatile state on all servers' the commitIndex is initialized to zero
          // so
          // we expect successive append entries calls to apply successive entries to the state machine
          // until commitIndex = lastApplied

          val persistentStorage =
            new InMemoryPersistentStorage[String](Vector(LogEntry("A", 1, index = 0),
                                                         LogEntry("B", 1, index = 1),
                                                         LogEntry("C", 1, index = 2)),
                                                  currentTerm = 1,
                                                  votedFor = "anyone")

          val _ = aRaftNode(persistentStorage)

          val appendResult = appendCallback(
            EntriesToAppend(term = 1,
                            leaderId = "anyone",
                            prevLogIndex = 2,
                            prevLogTerm = 1,
                            entries = List(LogEntry("D", 1, index = 3)),
                            leaderCommitIndex = 4))

          appendResult shouldBe AppendEntriesResult(term = 1, success = true)

          val orderVerify = inOrder(stateMachine)
          orderVerify.verify(stateMachine).apply("A")
          orderVerify.verify(stateMachine).apply("B")
          orderVerify.verify(stateMachine).apply("C")
          orderVerify.verify(stateMachine).apply("D")
        }
      }
      "implement rules for servers" should {
        "handle rule #1" in {
          // verified in the rule #5 test above
        }

        "handle rule #2 - if RPC request contains term T > currentTerm, set currentTerm = T" in {
          val persistentStorage =
            new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

          val raftNode = aRaftNode(persistentStorage)

          val appendResult = appendCallback(
            EntriesToAppend(term = 2,
                            leaderId = "anyone",
                            prevLogIndex = -1,
                            prevLogTerm = 1,
                            entries = List(),
                            leaderCommitIndex = -1))

          raftNode.getPersistentState shouldBe (2, "anyone")
          appendResult shouldBe AppendEntriesResult(term = 2, success = true)
        }
      }
    }
    "implemented by a Candidate" should {
      "stand down to follower if AppendEntries received from new leader" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

        val raftNode = aCandidate(persistentStorage)

        val appendResult = appendCallback(
          EntriesToAppend(term = 2,
                          leaderId = "i2",
                          prevLogIndex = -1,
                          prevLogTerm = 1,
                          entries = List(),
                          leaderCommitIndex = -1))

        raftNode.getRoleState shouldBe a[RaftConsensus.Follower[_]]
        appendResult shouldBe AppendEntriesResult(term = 2, success = true)
      }
      "reject leader append entries calls from a leader with a lower term" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "anyone")

        val raftNode = aCandidate(persistentStorage)

        val appendResult = appendCallback( // another server advises term 2
          EntriesToAppend(term = 2,
                          leaderId = "i2",
                          prevLogIndex = -1,
                          prevLogTerm = 1,
                          entries = List(),
                          leaderCommitIndex = -1))

        raftNode.getRoleState shouldBe a[RaftConsensus.Candidate[_]]
        appendResult shouldBe AppendEntriesResult(term = 3, success = false)
      }
    }
    "implemented by a Leader" should {
      "stand down if append entries received from a new leader" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "anyone")

        val raftNode = aLeader(persistentStorage)

        val appendResult = appendCallback( // another server advises term 3
          EntriesToAppend(term = 3,
                          leaderId = "i2",
                          prevLogIndex = -1,
                          prevLogTerm = 1,
                          entries = List(),
                          leaderCommitIndex = -1))

        raftNode.getRoleState shouldBe a[RaftConsensus.Follower[_]]
        appendResult shouldBe AppendEntriesResult(term = 3, success = true)
      }
      "reject leader append entries calls from a candidate with a lower term" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "anyone")

        val raftNode = aLeader(persistentStorage) // promotion to leader increments term to 3.

        val appendResult = appendCallback( // another server advises term 2
          EntriesToAppend(term = 1,
                          leaderId = "i2",
                          prevLogIndex = -1,
                          prevLogTerm = 1,
                          entries = List(),
                          leaderCommitIndex = -1))

        raftNode.getRoleState shouldBe a[RaftConsensus.Leader[_]]
        appendResult shouldBe AppendEntriesResult(term = 3, success = false)
      }
    }
  }
  // Rules for servers, followers, sec 5.2
  "Followers" when {
    "Receiving RPC requests" should {
      "Respond" in {
        // verified elsewhere
      }
    }

    "Election timeouts occur" should {
      "Convert to candidate" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

        val raftNode = aRaftNode(persistentStorage)

        timeoutCallback.apply()

        raftNode.getRoleState shouldBe a[RaftConsensus.Candidate[_]]

        // On conversion to candidate
        raftNode.getPersistentState shouldBe (2, raftNode.nodeId) // increment current term and vote for self

        verify(electionTimer).reset() // reset election timer

        val expectedVoteRequest = VoteRequested(2, "i1", -1, -1) // send request vote RPCs to other servers
        verify(rpc2).requestVote(expectedVoteRequest)
        verify(rpc3).requestVote(expectedVoteRequest)
      }
    }
  }
  "Candidates" when {
    "votes received from a majority" should {
      "Convert to leader" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

        val raftNode = aRaftNode(persistentStorage)

        // gets 2 out of 3 votes
        when(rpc2.requestVote(any[VoteRequested])).thenReturn(Future(RequestVoteResult(term = 2, voteGranted = true)))
        when(rpc3.requestVote(any[VoteRequested]))
          .thenReturn(Future(RequestVoteResult(term = 2, voteGranted = false)))

        timeoutCallback.apply()

        eventually { // after vote requests have come in
          raftNode.getRoleState shouldBe a[RaftConsensus.Leader[_]]
        }
      }
    }
    "votes received from a minority" should {
      "Remain a candidate" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

        val raftNode = aRaftNode(persistentStorage)

        // gets 1 out of 3 votes
        when(rpc2.requestVote(any[VoteRequested]))
          .thenReturn(Future(RequestVoteResult(term = 2, voteGranted = false)))
        when(rpc3.requestVote(any[VoteRequested]))
          .thenReturn(Future(RequestVoteResult(term = 2, voteGranted = false)))

        timeoutCallback.apply()

        after(500) {
          raftNode.getRoleState shouldBe a[RaftConsensus.Candidate[_]]
        }
      }
    }
  }
}

trait TestRPC extends BeforeAndAfterEach { this: Suite =>

  implicit val ec: ExecutionContext = ExecutionContexts.global
  implicit var actorSystem: ActorSystem = ActorSystem()
  implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 500 millis)

  override def beforeEach() {
    reset(peer, rpc2, rpc3, stateMachine, electionTimer)
    actorSystem = ActorSystem()
  }

  override def afterEach() {
    Await.result(actorSystem.terminate(), 1 second)
  }

  type Command = String
  val stateMachine: Command => Unit = mock[Command => Unit]
  val peer: RaftNode[Command] = mock[RaftNode[Command]]
  val rpc2: RPC[Command] = mock[RPC[Command]]
  val rpc3: RPC[Command] = mock[RPC[Command]]
  var appendCallback: EntriesToAppend[Command] => AppendEntriesResult = _
  var voteCallback: VoteRequested => RequestVoteResult = _
  val rpcFactory: RPCFactory[Command] = (node, appendEntriesCallback, requestVoteCallback) => {
    appendCallback = appendEntriesCallback
    voteCallback = requestVoteCallback
    if (node == "i2")
      rpc2
    else if (node == "i3")
      rpc3
    else
      fail("Test setup is wrong.")
  }
  var timeoutCallback: () => Unit = _
  val electionTimer: ElectionTimer = mock[ElectionTimer]
  val electionTimerFactory: ElectionTimerFactory = timeoutHandler => {
    timeoutCallback = timeoutHandler
    electionTimer
  }

  def after[T](millis: Long)(t: => T): Future[T] = {
    Future {
      Thread.sleep(millis)
    }.map(_ => t)
  }

  def aRaftNode(persistentStorage: PersistentStorage[Command]): RaftNode[Command] =
    new RaftNode[Command]("i1",
                          Set("i1", "i2", "i3"),
                          rpcFactory,
                          electionTimerFactory,
                          stateMachine,
                          persistentStorage)

  def aFollower(persistentStorage: PersistentStorage[Command]): RaftNode[Command] =
    aRaftNode(persistentStorage)

  def aCandidate(persistentStorage: PersistentStorage[Command]): RaftNode[Command] = {

    val raftNode = aRaftNode(persistentStorage)

    timeoutCallback.apply()

    raftNode.getRoleState shouldBe a[RaftConsensus.Candidate[_]]

    raftNode
  }

  def aLeader(persistentStorage: PersistentStorage[Command]): RaftNode[Command] = {
    val raftNode = aRaftNode(persistentStorage)

    val (term, _) = persistentStorage.state

    when(rpc2.requestVote(any[VoteRequested]))
      .thenReturn(Future(RequestVoteResult(term = term + 1, voteGranted = true)))
    when(rpc3.requestVote(any[VoteRequested]))
      .thenReturn(Future(RequestVoteResult(term = term + 1, voteGranted = true)))

    timeoutCallback.apply()

    eventually {
      raftNode.getRoleState shouldBe a[RaftConsensus.Leader[_]]
    }

    raftNode
  }
}

class InMemoryPersistentStorage[T](var logEntries: Vector[LogEntry[T]], var currentTerm: Int, var votedFor: String)
    extends PersistentStorage[T] {

  override def state: (Int, String) = (currentTerm, votedFor)

  override def log: Vector[LogEntry[T]] = logEntries
}

class LocalRPC[T](peer: RaftNode[T])(implicit ec: ExecutionContext) extends RPC[T] {

  override def appendEntries(entriesToAppend: EntriesToAppend[T]): Future[AppendEntriesResult] =
    Future(peer.appendEntries(entriesToAppend))

  override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] =
    Future(peer.requestVote(voteRequested))
}
