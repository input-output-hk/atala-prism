package io.iohk.cef.consensus.raft

import io.iohk.cef.consensus.raft.node._
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures.{convertScalaFuture, whenReady}
import org.scalatest.mockito.MockitoSugar._
import org.mockito.Mockito.{inOrder, times, verify, when}
import org.mockito.ArgumentMatchers.any
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class RaftConsensusSpec extends WordSpec {
  "Concurrency model" should {
    "prevent multiple votes for the same candidate" in new MockFixture {
      val persistentStorage =
        new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "")

      val _ = aRaftNode(persistentStorage)

      val nRequests = 25

      val voteRequests =
        Range(0, nRequests).map(i => VoteRequested(term = 3, candidateId = s"i$i", lastLogIndex = 0, lastLogTerm = 2))

      // spin up 100 very closely spaced requestVote RPCs
      val voteResultFs = Range(0, nRequests).map(i => Future(voteCallback(voteRequests(i))))

      whenReady(Future.sequence(voteResultFs)) { voteResults =>
        // only one vote should be granted.
        voteResults.count(result => result.voteGranted) shouldBe 1
      }
    }
    "provide sequencing of side effecting functions withRaftContext" in new MockFixture {
      val raftNode = aRaftNode(new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = ""))
      val nRequests = 10
      val sideEffect = mock[Int => Unit]

      def fnWithSideEffect(): Int = raftNode.withState { rs =>
        val currentState = rs.commonVolatileState
        val rs2 = rs.copy(commonVolatileState = currentState.copy(commitIndex = currentState.commitIndex + 1))
        sideEffect(rs2.commonVolatileState.commitIndex)
        (rs2, currentState.commitIndex + 1)
      }

      // spin up very closely spaced requests
      val resultsF = Range(0, nRequests).map(_ => Future(fnWithSideEffect()))

      whenReady(Future.sequence(resultsF)) { results =>
        Range(0, nRequests).foreach(i => verify(sideEffect, times(1)).apply(i))
        results.sorted shouldBe Range(0, nRequests) // sorted since we don't know which of the test Futures 'hit' the node first.
      }
    }
    "provide sequencing of side effecting Future functions with futureRaftContext" in new MockFixture {

      val raftNode = aRaftNode(new InMemoryPersistentStorage[String](Vector(), currentTerm = 0, votedFor = ""))
      val nRequests = 25
      val sideEffect = mock[Int => Unit]

      // a raft leader updates its commit index 'in the future' based on the responses of followers to log replication
      // RPCs. We create a simplified version of this op, which just increments the commitIndex by 1 on every invocation.
      // If invocations are correctly pipelined, the final commitIndex will equal the number of invocations.
      def futureFnWithSideEffects(): Future[Int] = raftNode.withFutureState { rs =>
        Future {
          val currentState = rs.commonVolatileState
          val rs2 = rs.copy(commonVolatileState = currentState.copy(commitIndex = currentState.commitIndex + 1))
          sideEffect(rs2.commonVolatileState.commitIndex)
          (rs2, currentState.commitIndex + 1)
        }
      }

      val incrementFs = Range(0, nRequests).map(_ => futureFnWithSideEffects())

      whenReady(Future.sequence(incrementFs)) { commitIndices =>
        Range(0, nRequests).foreach(i => verify(sideEffect, times(1)).apply(i))
        commitIndices shouldBe Range(0, nRequests)
      }
    }
  }
  "Consensus module" should {
    "Handles all client requests" when {
      "a client contacts a follower" should {
        "redirect to the leader" in new MockFixture {
          val raftNode = mock[RaftNode[String]]
          val apparentLeaderRpc = mock[RPC[String]]
          val realLeaderRpc = mock[RPC[String]]

          when(raftNode.getLeaderRPC).thenReturn(apparentLeaderRpc)
          when(apparentLeaderRpc.clientAppendEntries(any[Seq[String]]))
            .thenReturn(Future(Left(Redirect(realLeaderRpc))))
          when(realLeaderRpc.clientAppendEntries(any[Seq[String]])).thenReturn(Future(Right(())))

          val consensusModule = new RaftConsensus(raftNode)

          val responseF = consensusModule.appendEntries(Seq("A", "B", "C"))

          whenReady(responseF) { response =>
            verify(realLeaderRpc).clientAppendEntries(Seq("A", "B", "C"))
          }
        }
      }
      "a client contacts the leader" should {
        "handle the request" in new MockFixture {
          val raftNode = mock[RaftNode[String]]
          val leaderRpc = mock[RPC[String]]
          when(raftNode.getLeaderRPC).thenReturn(leaderRpc)
          when(leaderRpc.clientAppendEntries(any[Seq[String]])).thenReturn(Future(Right(())))

          val consensusModule = new RaftConsensus(raftNode)

          val responseF = consensusModule.appendEntries(Seq("A", "B", "C"))

          whenReady(responseF) { response =>
            verify(leaderRpc).clientAppendEntries(Seq("A", "B", "C"))
          }
        }
      }
    }
  }
  "AppendEntries RPC" when {
    "implemented by a Follower" should {
      "implement rule #1" should {
        "reply false if term < currentTerm)" in new MockFixture {

          val persistentStorage =
            new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

          val _ = aRaftNode(persistentStorage)
          val appendResult = appendCallback(EntriesToAppend(term = 0, leaderId = "anyone", 0, 0, List(), 1))

          appendResult shouldBe AppendEntriesResult(term = 1, success = false)
        }
      }
      "implement rule #2" should {
        "reply false if log doesn't contain an entry at prevLogIndex" in new MockFixture {

          val persistentStorage =
            new InMemoryPersistentStorage[String](
              Vector(LogEntry("A", 1, 0), LogEntry("B", 1, 1), LogEntry("C", 2, 2)),
              currentTerm = 1,
              votedFor = "anyone")

          val _ = aRaftNode(persistentStorage)

          val appendResult = appendCallback(
            EntriesToAppend(
              term = 1,
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
          "delete the existing entry and those that follow it" in new MockFixture {

          val persistentStorage =
            new InMemoryPersistentStorage[String](
              Vector(LogEntry("A", 1, 0), LogEntry("B", term = 1, 1), LogEntry("C", term = 1, 2)),
              currentTerm = 1,
              votedFor = "anyone")

          val raftNode = aRaftNode(persistentStorage)

          val appendResult = appendCallback(
            EntriesToAppend(
              term = 1,
              leaderId = "anyone",
              prevLogIndex = 1,
              prevLogTerm = 1,
              entries = List(LogEntry("B", term = 2, 1), LogEntry("C", term = 2, 2)),
              leaderCommitIndex = 4))

          appendResult shouldBe AppendEntriesResult(term = 1, success = false)
          persistentStorage.log shouldBe Vector(LogEntry("A", 1, 0))
        }
      }
      "implement rule #4" should {
        "append new entries not already in the log" in new MockFixture {

          val persistentStorage =
            new InMemoryPersistentStorage[String](
              Vector(LogEntry("A", 1, index = 0), LogEntry("B", 1, index = 1), LogEntry("C", 1, index = 2)),
              currentTerm = 1,
              votedFor = "anyone")

          val raftNode = aRaftNode(persistentStorage)

          val appendResult = appendCallback(
            EntriesToAppend(
              term = 1,
              leaderId = "anyone",
              prevLogIndex = 1,
              prevLogTerm = 1,
              entries = List(LogEntry("C", 1, index = 2), LogEntry("D", 1, index = 3)),
              leaderCommitIndex = 3))

          appendResult shouldBe AppendEntriesResult(term = 1, success = true)

          persistentStorage.log shouldBe Vector(
            LogEntry("A", 1, index = 0),
            LogEntry("B", 1, index = 1),
            LogEntry("C", 1, index = 2),
            LogEntry("D", 1, index = 3))
        }
      }
      "implement rule #5" should {
        "if leaderCommit > commitIndex set commitIndex = min(leaderCommit, index of last new entry)" in new MockFixture {
          // by 'Rules for servers', note for all servers,
          // if commitIndex > lastApplied, apply log[lastApplied] to state machine,
          // and
          // by 'state', 'volatile state on all servers' the commitIndex is initialized to zero
          // so
          // we expect successive append entries calls to apply successive entries to the state machine
          // until commitIndex = lastApplied

          val persistentStorage =
            new InMemoryPersistentStorage[String](
              Vector(LogEntry("A", 1, index = 0), LogEntry("B", 1, index = 1), LogEntry("C", 1, index = 2)),
              currentTerm = 1,
              votedFor = "anyone")

          val _ = aRaftNode(persistentStorage)

          val appendResult = appendCallback(
            EntriesToAppend(
              term = 1,
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
        "handle rule #1" in new MockFixture {
          // verified in the rule #5 test above
        }

        "handle rule #2 - if RPC request contains term T > currentTerm, set currentTerm = T" in new MockFixture {
          val persistentStorage =
            new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

          val raftNode = aRaftNode(persistentStorage)

          val appendResult = appendCallback(
            EntriesToAppend(
              term = 2,
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
//      "stand down to follower if AppendEntries received from new leader" in new MockFixture {
//        val persistentStorage =
//          new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")
//
//        val raftNode = aCandidate(persistentStorage)
//
//        val appendResult = appendCallback(
//          EntriesToAppend(
//            term = 2,
//            leaderId = "i2",
//            prevLogIndex = -1,
//            prevLogTerm = 1,
//            entries = List(),
//            leaderCommitIndex = -1))
//
//        appendResult shouldBe AppendEntriesResult(term = 2, success = true)
//        eventually { raftNode.getRole shouldBe Follower }
//      }
      "reject leader append entries calls from a leader with a lower term" in new MockFixture {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "anyone")

        val _ = aCandidate(persistentStorage)

        val appendResult = appendCallback( // another server advises term 2
          EntriesToAppend(
            term = 2,
            leaderId = "i2",
            prevLogIndex = -1,
            prevLogTerm = 1,
            entries = List(),
            leaderCommitIndex = -1))

        appendResult shouldBe AppendEntriesResult(term = 3, success = false)
      }
    }
    "implemented by a Leader" should {
      "stand down if append entries received from a new leader" in new MockFixture {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "anyone")

        val raftNode = aLeader(persistentStorage)

        val appendResult = appendCallback( // another server advises term 3
          EntriesToAppend(
            term = 3,
            leaderId = "i2",
            prevLogIndex = -1,
            prevLogTerm = 1,
            entries = List(),
            leaderCommitIndex = -1))

        raftNode.getRole shouldBe Follower
        appendResult shouldBe AppendEntriesResult(term = 3, success = true)
      }
      "reject leader append entries calls from a candidate with a lower term" in new MockFixture {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "anyone")

        val raftNode = aLeader(persistentStorage) // promotion to leader increments term to 3.

        val appendResult = appendCallback( // another server advises term 2
          EntriesToAppend(
            term = 1,
            leaderId = "i2",
            prevLogIndex = -1,
            prevLogTerm = 1,
            entries = List(),
            leaderCommitIndex = -1))

        raftNode.getRole shouldBe Leader
        appendResult shouldBe AppendEntriesResult(term = 3, success = false)
      }
    }
  }
  "RequestVote RPC" when {
    "implemented by any role" should {
      // NB: making use of the 'whitebox' knowledge that the RequestVote impl is common to all roles.
      "implement rule #1, reply false if term < currentTerm (sec 5.1)" in new MockFixture {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "i2")

        val _ = aRaftNode(persistentStorage)

        val voteResult = voteCallback(VoteRequested(term = 1, candidateId = "i2", lastLogIndex = 0, lastLogTerm = 1))

        voteResult shouldBe RequestVoteResult(term = 2, voteGranted = false)
      }
      "implement rule #2" when {

        "votedFor is null and candidate's log is as up to date as the receiver's log" should {
          "grant vote" in new MockFixture {
            val persistentStorage =
              new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "")

            val _ = aRaftNode(persistentStorage)

            val voteResult =
              voteCallback(VoteRequested(term = 3, candidateId = "i2", lastLogIndex = 0, lastLogTerm = 2))

            voteResult shouldBe RequestVoteResult(term = 3, voteGranted = true)
          }
        }
        "votedFor is candidateId and candidate's log is as up to date as the receiver's log" should {
          "grant vote" in new MockFixture {
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
          "deny vote" in new MockFixture {
            val persistentStorage =
              new InMemoryPersistentStorage[String](
                Vector(LogEntry("A", 1, 0), LogEntry("B", 1, 1), LogEntry("C", 2, 2)),
                currentTerm = 2,
                votedFor = "i2")

            val _ = aRaftNode(persistentStorage)

            val voteResult =
              voteCallback(VoteRequested(term = 3, candidateId = "i2", lastLogIndex = 2, lastLogTerm = 1))

            voteResult shouldBe RequestVoteResult(term = 3, voteGranted = false)
          }
        }
        "candidate's log has lower lastLogLogIndex than the receiver's log" should {
          "deny vote" in new MockFixture {
            val persistentStorage =
              new InMemoryPersistentStorage[String](
                Vector(LogEntry("A", 1, 0), LogEntry("B", 1, 1), LogEntry("C", 2, 2)),
                currentTerm = 2,
                votedFor = "i2")

            val _ = aRaftNode(persistentStorage)

            val voteResult =
              voteCallback(VoteRequested(term = 3, candidateId = "i2", lastLogIndex = 1, lastLogTerm = 2))

            voteResult shouldBe RequestVoteResult(term = 3, voteGranted = false)
          }
        }
        "votedFor is NOT candidateId" should {
          "deny vote" in new MockFixture {
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
      "Respond" in new MockFixture {
        // verified elsewhere
      }
    }

    "Election timeouts occur" should {
      "Implement Rules for Servers, Candidates conversion rules" in new MockFixture {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 1, votedFor = "anyone")

        val raftNode = aRaftNode(persistentStorage)
        when(rpc2.requestVote(any[VoteRequested])).thenReturn(Future(RequestVoteResult(term = 2, voteGranted = false)))
        when(rpc3.requestVote(any[VoteRequested])).thenReturn(Future(RequestVoteResult(term = 2, voteGranted = false)))

        electionTimeoutCallback.apply()

        raftNode.getRole shouldBe Candidate

        // On conversion to candidate
        raftNode.getPersistentState shouldBe (2, raftNode.nodeId) // 1. increment current term and 2. vote for self

        verify(electionTimer).reset() // 3. reset election timer

        val expectedVoteRequest = VoteRequested(2, "i1", -1, -1) // 4. send request vote RPCs to other servers
        eventually {
          verify(rpc2).requestVote(expectedVoteRequest)
          verify(rpc3).requestVote(expectedVoteRequest)
        }
      }
    }
  }
  "Candidates" when {
    "votes received from a majority" should {
      "Implement Candidates note #2, become leader" in new MockFixture {
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
          raftNode.getRole shouldBe Leader
        }
      }
    }
    "votes received from a minority" should {
      "Remain a candidate" in new MockFixture { // TODO is this correct?
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
          raftNode.getRole shouldBe Candidate
        }
      }
    }
    // See AppendEntries RPC candidate tests for tests of note #3
  }
  "Leaders" when {
    "Winning an election" should {
      "Implement Leaders note #1, send initial empty AppendEntries RPCs to each server and repeat during idle periods" in new MockFixture {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "i1")

        val _ = aLeader(persistentStorage)

        val expectedHeartbeat = EntriesToAppend(
          term = 3,
          leaderId = "i1",
          prevLogIndex = -1,
          prevLogTerm = -1,
          entries = Seq[LogEntry[String]](),
          leaderCommitIndex = -1)

        heartbeatTimeoutCallback.apply()
        heartbeatTimeoutCallback.apply()
        eventually {
          verify(rpc2, times(3)).appendEntries(expectedHeartbeat)
          verify(rpc3, times(3)).appendEntries(expectedHeartbeat)
        }
      }
    }
    "Receiving commands from a client" should {
      "Implement Leaders note #2, append entry to local log, apply to state machine and respond to client" in new MockFixture {
        val persistentStorage =
          new InMemoryPersistentStorage[String](Vector(), currentTerm = 2, votedFor = "i1")

        val raftNode = aLeader(persistentStorage)

        when(rpc2.appendEntries(any[EntriesToAppend[Command]]))
          .thenReturn(Future(AppendEntriesResult(3, success = true)))
        when(rpc3.appendEntries(any[EntriesToAppend[Command]]))
          .thenReturn(Future(AppendEntriesResult(3, success = true)))

        val response = raftNode.clientAppendEntries(Seq("A")).futureValue
        response shouldBe Right(())
        persistentStorage.log.last.command shouldBe "A"
        verify(stateMachine).apply("A")
        raftNode.getCommonVolatileState shouldBe CommonVolatileState(0, 0)
      }
      "Implement Leaders note #3 and #4" when {
        "last log index >= nextIndex for a follower" should {
          "send AppendEntries RPC with entries starting at nextIndex" in new MockFixture {
            val persistentStorage =
              new InMemoryPersistentStorage[String](
                Vector(LogEntry[String]("A", 2, 0)),
                currentTerm = 2,
                votedFor = "i1")

            val raftNode = aLeader(persistentStorage)
            val heartbeat = EntriesToAppend[String](3, "i1", 0, 2, Seq(), -1)

            verify(rpc2).appendEntries(heartbeat) // the initial heartbeat
            verify(rpc3).appendEntries(heartbeat)

            val expectedAppendEntries =
              EntriesToAppend(3, "i1", 0, 2, Seq(LogEntry[String]("B", 3, 1), LogEntry[String]("C", 3, 2)), -1)

            when(rpc2.appendEntries(expectedAppendEntries)).thenReturn(Future(AppendEntriesResult(3, success = true)))
            when(rpc3.appendEntries(expectedAppendEntries)).thenReturn(Future(AppendEntriesResult(3, success = false)))

            // the adjusted call to node i3.
            val expectedAdjustedAppendEntries =
              EntriesToAppend(
                3,
                "i1",
                -1,
                -1,
                Seq(LogEntry[String]("A", 2, 0), LogEntry[String]("B", 3, 1), LogEntry[String]("C", 3, 2)),
                -1)

            when(rpc3.appendEntries(expectedAdjustedAppendEntries))
              .thenReturn(Future(AppendEntriesResult(3, success = true)))

            val response: Either[Redirect[String], Unit] = raftNode.clientAppendEntries(Seq("B", "C")).futureValue

            response shouldNot be(a[Redirect[_]])

            // the initial rpc following the client request
            verify(rpc2).appendEntries(expectedAppendEntries)
            verify(rpc3).appendEntries(expectedAppendEntries)
            verify(rpc3).appendEntries(expectedAdjustedAppendEntries)

            val leaderVolatileState = raftNode.getLeaderVolatileState
            leaderVolatileState.nextIndex shouldBe Seq(3, 3)
            leaderVolatileState.matchIndex shouldBe Seq(2, 2)
          }
        }
      }
    }
  }
  "In an integrated cluster" when {
    "servers are started" should {
      "elect a leader" in new IntegratedFixture {
        val s1 = new InMemoryPersistentStorage[String](Vector(), 1, "")
        val s2 = new InMemoryPersistentStorage[String](Vector(), 1, "")
        val s3 = new InMemoryPersistentStorage[String](Vector(), 1, "")

        val (t1, t2, t3) = anIntegratedCluster(s1, s2, s3)
        t1.electionTimeoutCallback.apply()
        eventually {
          t1.raftNode.getRole shouldBe Leader
        }
      }
      "replicate logs" in new IntegratedFixture {
        val s1 = new InMemoryPersistentStorage[String](Vector(), 1, "")
        val s2 = new InMemoryPersistentStorage[String](Vector(), 1, "")
        val s3 = new InMemoryPersistentStorage[String](Vector(), 1, "")

        val (t1, t2, t3) = anIntegratedCluster(s1, s2, s3)
        t1.electionTimeoutCallback.apply()

        eventually {
          t1.raftNode.getRole shouldBe Leader
        }

        val appendResult = t1.raftNode.clientAppendEntries(Seq("A", "B", "C", "D", "E")).futureValue

        appendResult shouldBe Right(())

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

        t1.heartbeatTimeoutCallback.apply()

        eventually {
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
}

trait MockFixture {

  implicit val ec: ExecutionContext = ExecutionContext.global
  val timeout = Span(5, Seconds)
  val span = Span(50, Millis)
  implicit val eventuallyPatienceConfig: Eventually.PatienceConfig =
    Eventually.PatienceConfig(timeout = timeout, interval = span)
  implicit val futuresPatienceConfig: ScalaFutures.PatienceConfig =
    ScalaFutures.PatienceConfig(timeout = timeout, interval = span)

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
    new RaftNode[Command](
      "i1",
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
    eventually {
      raftNode.getRole shouldBe Candidate
    }

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

    eventually {
      raftNode.getRole shouldBe Leader
    }

    raftNode
  }

}

trait IntegratedFixture {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val timeout = Span(5, Seconds)
  val span = Span(50, Millis)

  implicit val eventuallyPatienceConfig: Eventually.PatienceConfig =
    Eventually.PatienceConfig(timeout = timeout, interval = span)
  implicit val futuresPatienceConfig: ScalaFutures.PatienceConfig =
    ScalaFutures.PatienceConfig(timeout = timeout, interval = span)

  type Command = String

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
      new RaftNode[Command](
        nodeId,
        clusterIds,
        localRpcFactory,
        electionTimerFactory,
        heartbeatTimerFactory,
        machine,
        state)

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

class InMemoryPersistentStorage[T](var logEntries: Vector[LogEntry[T]], var currentTerm: Int, var votedFor: String)
    extends PersistentStorage[T] {

  override def state: (Int, String) = (currentTerm, votedFor)

  override def log: Vector[LogEntry[T]] = logEntries

  override def state(currentTerm: Int, votedFor: String): Unit = {
    this.currentTerm = currentTerm
    this.votedFor = votedFor
  }
  override def log(deletes: Int, writes: Seq[LogEntry[T]]): Unit = {
    logEntries = logEntries.dropRight(deletes) ++ writes
  }
}

class LocalRPC[T](peer: => RaftNode[T])(implicit ec: ExecutionContext) extends RPC[T] {

  override def appendEntries(entriesToAppend: EntriesToAppend[T]): Future[AppendEntriesResult] = {
    Future(peer.appendEntries(entriesToAppend))
  }

  override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] =
    Future(peer.requestVote(voteRequested))

  override def clientAppendEntries(entries: Seq[T]): Future[Either[Redirect[T], Unit]] =
    peer.clientAppendEntries(entries)
}
