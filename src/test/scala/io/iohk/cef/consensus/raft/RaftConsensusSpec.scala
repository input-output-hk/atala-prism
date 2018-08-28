package io.iohk.cef.consensus.raft

import java.util.{Timer, TimerTask}

import io.iohk.cef.consensus.raft.RaftConsensus.{VoteRequested, _}
import org.scalatest.{BeforeAndAfterEach, Suite, WordSpec}
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar._
import org.mockito.Mockito.{inOrder, reset, times, verify, when}
import org.mockito.ArgumentMatchers.any

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

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
  "RequestVote RPC" when {
    "implemented by any role" should {
      // NB: making use of the 'whitebox' knowledge that the RequestVote impl is common to all roles.
      "implement rule #1, reply false if term < currentTerm (sec 5.1)" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "i2")

        val _ = aRaftNode(persistentStorage)

        val voteResult = voteCallback(VoteRequested(term = 1, candidateId = "i2", lastLogIndex = 0, lastLogTerm = 1))

        voteResult shouldBe RequestVoteResult(term = 2, voteGranted = false)
      }
      "implement rule #2" when {

        "votedFor is null and candidate's log is as up to date as the receiver's log" should {
          "grant vote" in {
            val persistentStorage =
              new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "")

            val _ = aRaftNode(persistentStorage)

            val voteResult =
              voteCallback(VoteRequested(term = 3, candidateId = "i2", lastLogIndex = 0, lastLogTerm = 2))

            voteResult shouldBe RequestVoteResult(term = 3, voteGranted = true)
          }
        }
        "votedFor is candidateId and candidate's log is as up to date as the receiver's log" should {
          "grant vote" in {
            val persistentStorage =
              new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "i2")

            val _ = aRaftNode(persistentStorage)

            val voteResult =
              voteCallback(VoteRequested(term = 3, candidateId = "i2", lastLogIndex = 0, lastLogTerm = 2))

            voteResult shouldBe RequestVoteResult(term = 3, voteGranted = true)
          }
        }
        /*
        Sec 5.4.1
        If the logs have last entries with different terms, then
        the log with the later term is more up-to-date. If the logs
        end with the same term, then whichever log is longer is
        more up-to-date
         */
        "candidate's log has lower term than the receiver's log" should {
          "deny vote" in {
            val persistentStorage =
              new InMemoryPersistentStorage[String](Vector(LogEntry("A", 1, 0),
                                                           LogEntry("B", 1, 1),
                                                           LogEntry("C", 2, 2)),
                                                    currentTerm = 2,
                                                    votedFor = "i2")

            val _ = aRaftNode(persistentStorage)

            val voteResult =
              voteCallback(VoteRequested(term = 3, candidateId = "i2", lastLogIndex = 2, lastLogTerm = 1))

            voteResult shouldBe RequestVoteResult(term = 3, voteGranted = false)
          }
        }
        "candidate's log has lower lastLogLogIndex than the receiver's log" should {
          "deny vote" in {
            val persistentStorage =
              new InMemoryPersistentStorage[String](Vector(LogEntry("A", 1, 0),
                                                           LogEntry("B", 1, 1),
                                                           LogEntry("C", 2, 2)),
                                                    currentTerm = 2,
                                                    votedFor = "i2")

            val _ = aRaftNode(persistentStorage)

            val voteResult =
              voteCallback(VoteRequested(term = 3, candidateId = "i2", lastLogIndex = 1, lastLogTerm = 2))

            voteResult shouldBe RequestVoteResult(term = 3, voteGranted = false)
          }
        }
        "votedFor is NOT candidateId" should {
          "deny vote" in {
            val persistentStorage =
              new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "i3")

            val _ = aRaftNode(persistentStorage)

            val voteResult =
              voteCallback(VoteRequested(term = 3, candidateId = "i2", lastLogIndex = 0, lastLogTerm = 2))

            voteResult shouldBe RequestVoteResult(term = 3, voteGranted = false)
          }
        }
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
      "Implement Rules for Servers, Candidates conversion rules" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

        val raftNode = aRaftNode(persistentStorage)
        when(rpc2.requestVote(any[VoteRequested])).thenReturn(Future(RequestVoteResult(term = 2, voteGranted = false)))
        when(rpc3.requestVote(any[VoteRequested])).thenReturn(Future(RequestVoteResult(term = 2, voteGranted = false)))

        electionTimeoutCallback.apply()

        raftNode.getRoleState shouldBe a[RaftConsensus.Candidate[_]]

        // On conversion to candidate
        raftNode.getPersistentState shouldBe (2, raftNode.nodeId) // 1. increment current term and 2. vote for self

        verify(electionTimer).reset() // 3. reset election timer

        val expectedVoteRequest = VoteRequested(2, "i1", -1, -1) // 4. send request vote RPCs to other servers
        verify(rpc2).requestVote(expectedVoteRequest)
        verify(rpc3).requestVote(expectedVoteRequest)
      }
    }
  }
  "Candidates" when {
    "votes received from a majority" should {
      "Implement Candidates note #2, become leader" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

        val raftNode = aRaftNode(persistentStorage)

        // gets 2 out of 3 votes
        when(rpc2.requestVote(any[VoteRequested])).thenReturn(Future(RequestVoteResult(term = 2, voteGranted = true)))
        when(rpc3.requestVote(any[VoteRequested])).thenReturn(Future(RequestVoteResult(term = 2, voteGranted = false)))

        when(rpc2.appendEntries(any[EntriesToAppend[Command]]))
          .thenReturn(Future(AppendEntriesResult(2, success = true)))
        when(rpc3.appendEntries(any[EntriesToAppend[Command]]))
          .thenReturn(Future(AppendEntriesResult(2, success = true)))


        electionTimeoutCallback.apply()

        eventually { // after vote requests have come in
          raftNode.getRoleState shouldBe a[RaftConsensus.Leader[_]]
        }
      }
    }
    "votes received from a minority" should {
      "Remain a candidate" in { // TODO is this correct?
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

        val raftNode = aRaftNode(persistentStorage)

        // gets 1 out of 3 votes
        when(rpc2.requestVote(any[VoteRequested]))
          .thenReturn(Future(RequestVoteResult(term = 2, voteGranted = false)))
        when(rpc3.requestVote(any[VoteRequested]))
          .thenReturn(Future(RequestVoteResult(term = 2, voteGranted = false)))

        electionTimeoutCallback.apply()

        after(500) {
          raftNode.getRoleState shouldBe a[RaftConsensus.Candidate[_]]
        }
      }
    }
    // See AppendEntries RPC candidate tests for tests of note #3
  }
  "Leaders" when {
    "Winning an election" should {
      "Implement Leaders note #1, send initial empty AppendEntries RPCs to each server and repeat during idle periods" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "i1")

        val _ = aLeader(persistentStorage)

        val expectedHeartbeat = EntriesToAppend(term = 3,
                                                leaderId = "i1",
                                                prevLogIndex = -1,
                                                prevLogTerm = -1,
                                                entries = Seq[LogEntry[String]](),
                                                leaderCommitIndex = -1)

        heartbeatTimeoutCallback.apply()
        heartbeatTimeoutCallback.apply()

        verify(rpc2, times(3)).appendEntries(expectedHeartbeat)
        verify(rpc3, times(3)).appendEntries(expectedHeartbeat)
      }
    }
    "Receiving commands from a client" should {
      "Implement Leaders note #2, append entry to local log, apply to state machine and respond to client" in {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "i1")

        val raftNode = aLeader(persistentStorage)

        when(rpc2.appendEntries(any[EntriesToAppend[Command]]))
          .thenReturn(Future(AppendEntriesResult(3, success = true)))
        when(rpc3.appendEntries(any[EntriesToAppend[Command]]))
          .thenReturn(Future(AppendEntriesResult(3, success = true)))

        val response = raftNode.clientAppendEntries(Seq("A"))
        response shouldBe Right(())
        raftNode.getLog.last.command shouldBe "A"
        verify(stateMachine).apply("A")
        raftNode.getCommonVolatileState shouldBe CommonVolatileState(0, 0)
      }
      "Implement Leaders note #3 and #4" when {
        "last log index >= nextIndex for a follower" should {
          "send AppendEntries RPC with entries starting at nextIndex" in {
            val persistentStorage =
              new InMemoryPersistentStorage[String](Vector(LogEntry[String]("A", 2, 0)),
                                                    currentTerm = 2,
                                                    votedFor = "i1")

            val raftNode = aLeader(persistentStorage)

            verify(rpc2).appendEntries(any[EntriesToAppend[Command]]) // the initial heartbeat
            verify(rpc3).appendEntries(any[EntriesToAppend[Command]])

            val expectedAppendEntries =
              EntriesToAppend(3, "i1", 0, 2, Seq(LogEntry[String]("B", 3, 1), LogEntry[String]("C", 3, 2)), -1)

            when(rpc2.appendEntries(expectedAppendEntries)).thenReturn(Future(AppendEntriesResult(3, success = true)))
            when(rpc3.appendEntries(expectedAppendEntries)).thenReturn(Future(AppendEntriesResult(3, success = false)))

            // the adjusted call to node i3.
            val expectedAdjustedAppendEntries =
              EntriesToAppend(3,
                              "i1",
                              -1,
                              -1,
                              Seq(LogEntry[String]("A", 2, 0),
                                  LogEntry[String]("B", 3, 1),
                                  LogEntry[String]("C", 3, 2)),
                              -1)

            when(rpc3.appendEntries(expectedAdjustedAppendEntries))
              .thenReturn(Future(AppendEntriesResult(3, success = true)))

            val response: Either[Redirect, Unit] = raftNode.clientAppendEntries(Seq("B", "C"))

            response shouldNot be(a[Redirect])

            // the initial rpc following the client request
            verify(rpc2).appendEntries(expectedAppendEntries)
            verify(rpc3).appendEntries(expectedAppendEntries)
            verify(rpc3).appendEntries(expectedAdjustedAppendEntries)

            val leaderVolatileState = raftNode.leaderVolatileState.single()
            leaderVolatileState.nextIndex shouldBe Seq(3, 3)
            leaderVolatileState.matchIndex shouldBe Seq(2, 2)
          }
        }
      }
    }
  }
  "In an integrated cluster" when {
    "servers are started" should {
      "elect a leader" in {
        val s1 = new InMemoryPersistentStorage[String](Vector(), 1, "")
        val s2 = new InMemoryPersistentStorage[String](Vector(), 1, "")
        val s3 = new InMemoryPersistentStorage[String](Vector(), 1, "")

        val (t1, t2, t3) = anIntegratedCluster(s1, s2, s3)
        t1.electionTimeoutCallback.apply()

        t1.raftNode.getRoleState shouldBe a[RaftConsensus.Leader[_]]
      }
      "replicate logs" in {
        val s1 = new InMemoryPersistentStorage[String](Vector(), 1, "")
        val s2 = new InMemoryPersistentStorage[String](Vector(), 1, "")
        val s3 = new InMemoryPersistentStorage[String](Vector(), 1, "")

        val (t1, t2, t3) = anIntegratedCluster(s1, s2, s3)
        t1.electionTimeoutCallback.apply()

        val appendResult = t1.raftNode.clientAppendEntries(Seq("A", "B", "C", "D", "E"))

        appendResult shouldBe Right(())

        val expectedEntries = Vector(LogEntry("A", 2, 0),
                                     LogEntry("B", 2, 1),
                                     LogEntry("C", 2, 2),
                                     LogEntry("D", 2, 3),
                                     LogEntry("E", 2, 4))
        t1.raftNode.getLog shouldBe expectedEntries
        t2.raftNode.getLog shouldBe expectedEntries
        t3.raftNode.getLog shouldBe expectedEntries

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

        t1.heartbeatTimeoutCallback.apply()

        Seq("A", "B", "C", "D", "E").foreach(command => {
          verify(t2.machine).apply(command)
          verify(t3.machine).apply(command)
        })

        t2.raftNode.getCommonVolatileState shouldBe CommonVolatileState(4, 4)
        t3.raftNode.getCommonVolatileState shouldBe CommonVolatileState(4, 4)
      }
    }
  }
}

trait TestRPC extends BeforeAndAfterEach { this: Suite =>

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 500 millis)

  override def beforeEach() {
    reset(peer, rpc2, rpc3, stateMachine, electionTimer)
  }

//  override def afterEach() {}

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
  var electionTimeoutCallback: () => Unit = _
  val electionTimer: RaftTimer = mock[RaftTimer]
  val electionTimerFactory: RaftTimerFactory = timeoutHandler => {
    electionTimeoutCallback = timeoutHandler
    electionTimer
  }

  var heartbeatTimeoutCallback: () => Unit = _
  val heartbeatTimer: RaftTimer = mock[RaftTimer]
  val heartbeatTimerFactory: RaftTimerFactory = timeoutHandler => {
    heartbeatTimeoutCallback = timeoutHandler
    heartbeatTimer
  }

  def after[T](millis: Long)(t: => T): Future[T] = {
    Future {
      Thread.sleep(millis)
    }.map(_ => t)
  }

  def aRaftNode(persistentStorage: PersistentStorage[Command]): RaftNode[Command] =
    new RaftNode[Command]("i1",
                          Seq("i1", "i2", "i3"),
                          rpcFactory,
                          electionTimerFactory,
                          heartbeatTimerFactory,
                          stateMachine,
                          persistentStorage)

  def aFollower(persistentStorage: PersistentStorage[Command]): RaftNode[Command] =
    aRaftNode(persistentStorage)

  def aCandidate(persistentStorage: PersistentStorage[Command]): RaftNode[Command] = {

    val raftNode = aRaftNode(persistentStorage)

    val (term, _) = persistentStorage.state

    when(rpc2.requestVote(any[VoteRequested]))
      .thenReturn(Future(RequestVoteResult(term = term + 1, voteGranted = false)))
    when(rpc3.requestVote(any[VoteRequested]))
      .thenReturn(Future(RequestVoteResult(term = term + 1, voteGranted = false)))

    electionTimeoutCallback.apply()

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

    when(rpc2.appendEntries(any[EntriesToAppend[Command]]))
      .thenReturn(Future(AppendEntriesResult(term + 1, success = true)))
    when(rpc3.appendEntries(any[EntriesToAppend[Command]]))
      .thenReturn(Future(AppendEntriesResult(term + 1, success = true)))

    electionTimeoutCallback.apply()

    raftNode.getRoleState shouldBe a[RaftConsensus.Leader[_]]

    raftNode
  }

  val testNodes = mutable.Map[String, TestNode]()

  val clusterIds = Seq("i1", "i2", "i3")

  class TestNode(nodeId: String, state: PersistentStorage[Command]) {

    val machine: Command => Unit = mock[Command => Unit]

    var electionTimeoutCallback: () => Unit = _
    val electionTimer: RaftTimer = mock[RaftTimer]
    val electionTimerFactory: RaftTimerFactory = timeoutHandler => {
      electionTimeoutCallback = timeoutHandler
      electionTimer
    }

    var heartbeatTimeoutCallback: () => Unit = _
    val heartbeatTimer: RaftTimer = mock[RaftTimer]
    val heartbeatTimerFactory: RaftTimerFactory = timeoutHandler => {
      heartbeatTimeoutCallback = timeoutHandler
      heartbeatTimer
    }

    def localRpcFactory: RPCFactory[Command] = (nodeId, _, _) => {
      new LocalRPC[Command](testNodes(nodeId).raftNode)
    }

    lazy val raftNode: RaftNode[Command] =
      new RaftNode[Command](nodeId,
                            clusterIds,
                            localRpcFactory,
                            electionTimerFactory,
                            heartbeatTimerFactory,
                            machine,
                            state)

    testNodes.put(nodeId, this)
  }

  def anIntegratedCluster(i1State: PersistentStorage[Command],
                          i2State: PersistentStorage[Command],
                          i3State: PersistentStorage[Command]): (TestNode, TestNode, TestNode) = {

    val (t1, t2, t3) = (new TestNode("i1", i1State), new TestNode("i2", i2State), new TestNode("i3", i3State))
    t1.raftNode
    t2.raftNode
    t3.raftNode
    (t1, t2, t3)
  }
}

class InMemoryPersistentStorage[T](var logEntries: Vector[LogEntry[T]], var currentTerm: Int, var votedFor: String)
    extends PersistentStorage[T] {

  override def state: (Int, String) = (currentTerm, votedFor)

  override def log: Vector[LogEntry[T]] = logEntries
}

class LocalRPC[T](peer: => RaftNode[T])(implicit ec: ExecutionContext) extends RPC[T] {

  override def appendEntries(entriesToAppend: EntriesToAppend[T]): Future[AppendEntriesResult] =
    Future(peer.appendEntries(entriesToAppend))

  override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] =
    Future(peer.requestVote(voteRequested))
}

class BouncyTimer(minTimeout: Duration, maxTimeout: Duration)(timeoutFn: () => Unit) extends RaftTimer {

  val timer = new Timer()

  override def reset(): Unit = {
    timer.cancel()
    schedule()
  }

  private def schedule(): Unit = {
    timer.schedule(new TimerTask { override def run(): Unit = timeout() }, nextRandom())
  }

  private def timeout(): Unit = {
    timeoutFn()
    schedule()
  }

  private def nextRandom(): Int = {
    val minTimeoutMillis = minTimeout.toMillis.toInt
    val maxTimeoutMillis = maxTimeout.toMillis.toInt
    minTimeoutMillis + Random.nextInt((maxTimeoutMillis - minTimeoutMillis) + 1)
  }
}
