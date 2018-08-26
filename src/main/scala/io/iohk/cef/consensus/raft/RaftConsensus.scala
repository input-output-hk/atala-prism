package io.iohk.cef.consensus.raft

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup, unhandled}
import io.iohk.cef.utils.StateActor

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

object RaftConsensus {

  case class LogEntry[Command](command: Command, term: Int, index: Int)

  /**
    * To be implemented by users of the module using whatever networking method is relevant for them.
    *
    * @param node identifier of the node at the other end of the RPC
    * @param entriesAppended callback to invoke when another node sends an appendEntries RPC
    * @param voteRequested callback to invoke when another node sends a voteRequested RPC
    * @tparam Command the user command type.
    */
  type RPCFactory[Command] = (String,
    EntriesToAppend[Command] => Future[AppendEntriesResult],
    VoteRequested => Future[RequestVoteResult]) => RPC[Command]

  trait RPC[Command] {

    def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult]

    def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult]
  }


  trait PersistentStorage[Command] {
    def state: Future[(Int, String)]
    def state(currentTerm: Int, votedFor: String): Future[Unit]

    def log(entry: LogEntry[Command]): Future[Unit]
    def log: Future[Vector[LogEntry[Command]]]

    def dropRight(n: Int): Future[Unit]
  }

  case class Redirect(leader: String)

  type ElectionTimerFactory = (() => Unit) => ElectionTimer

  trait ElectionTimer {
    def reset(): Unit
  }

  /**
    * From Figure 1.
    * TODO new entries must go via the leader, so the module will need
    * to talk to the correct node.
    */
  class ConsensusModule[Command](raftNode: RaftNode[Command]) {
    // Sec 5.1
    // The leader handles all client requests (if
    // a client contacts a follower, the follower redirects it to the leader)
    def appendEntries(entries: Seq[Command]): Future[Either[Redirect, Unit]] = ???
  }

  class RaftNode[Command](val nodeId: String,
                          val clusterMemberIds: Set[String],
                          rpcFactory: RPCFactory[Command],
                          electionTimerFactory: ElectionTimerFactory,
                          stateMachine: Command => Unit,
                          val persistentStorage: PersistentStorage[Command])(implicit actorSystem: ActorSystem) {

    private implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    private val electionTimer = electionTimerFactory(() => electionTimeout())

    private val clusterMembers: Set[RPC[Command]] = clusterMemberIds
      .filterNot(_ == nodeId)
      .map(rpcFactory(_, appendEntries, requestVote))

    val roleState =
      new StateActor[NodeState[Command]]()

    val commonVolatileState = new StateActor[CommonVolatileState[Command]]()

    val leaderVolatileState = new StateActor[LeaderVolatileState]()

    val nodeFSM: ActorRef[NodeEvent] =
      actorSystem.spawnAnonymous(new NodeFSM[Command](becomeFollower, becomeCandidate, becomeLeader).stateMachine)

    def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] = {
      for {
        _ <- rulesForServersAllServers2(entriesToAppend.term)
        roleState <- roleState.get
        appendResult <- roleState.appendEntries(entriesToAppend)
        commonVolatileState <- commonVolatileState.get
        log <- persistentStorage.log
        _ <- applyUncommittedLogEntries(commonVolatileState, log)
      } yield
        appendResult
    }

    def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] =
      roleState.get.flatMap(state => state.requestVote(voteRequested))

    private def electionTimeout(): Unit =
      nodeFSM ! ElectionTimeout


    def requestVotes(voteRequested: VoteRequested): Future[Seq[RequestVoteResult]] = {
      val rpcFutures = clusterMembers.toSeq.map(memberRpc => memberRpc.requestVote(voteRequested))
      Future.sequence(rpcFutures)
    }

    def applyEntry(iEntry: Int, log: Vector[LogEntry[Command]]): Unit =
      stateMachine(log(iEntry).command)

    // Rules for servers, all servers, note 1.
    // If commitIndex > lastApplied, apply log[lastApplied] to the state machine.
    // (do this recursively until commitIndex == lastApplied)
    private def applyUncommittedLogEntries(state: CommonVolatileState[Command],
                                           log: Vector[LogEntry[Command]]): Future[Unit] = {

      val commitIndex = state.commitIndex
      val lastApplied = state.lastApplied

      if (commitIndex > lastApplied) {
        val nextApplication = lastApplied + 1
        val nextState: CommonVolatileState[Command] = state.copy(lastApplied = nextApplication)
        commonVolatileState
          .set(nextState)
          .map(_ => applyEntry(nextApplication, log))
          .flatMap(_ => applyUncommittedLogEntries(nextState, log))
      } else {
        commonVolatileState.get.map(_ => ())
      }
    }

    // Rules for servers (figure 2), all servers, note 2.
    // If request (or response) contains term T > currentTerm
    // set currentTerm = T (convert to follower)
    private def rulesForServersAllServers2(term: Int): Future[(Int, String)] = {
      val updateF = for {
        (currentTerm, votedFor) <- persistentStorage.state
        if term > currentTerm
        _ <- persistentStorage.state(term, votedFor)
      } yield {
        nodeFSM ! NodeWithHigherTermDiscovered
        (term, votedFor)
      }

      updateF.fallbackTo(persistentStorage.state)
    }

    // Note, this is different to the paper which specifies
    // one-based array ops, whereas we use zero-based.
    private def initialState(): Future[NodeState[Command]] = {
      val initialCommitIndex = -1
      val initialLastApplied = -1

      commonVolatileState
        .set(CommonVolatileState(initialCommitIndex, initialLastApplied))
        .map(_ => new Follower[Command](raftNode = this))
    }

    private def becomeFollower(event: NodeEvent): Future[Unit] = event match {
      case Start =>
        initialState().flatMap(state => roleState.set(state))
      case _ =>
        roleState.set(new Follower(this))
    }

    // On conversion to candidate, start election:
    // Increment currentTerm
    // Vote for self
    // Reset election timer
    // Send RequestVote RPCs to all other servers
    private def becomeCandidate(event: NodeEvent): Future[Unit] = {
      electionTimer.reset()
      for {
        _ <- roleState.set(new Candidate(this))
        (currentTerm, _) <- persistentStorage.state
        newTerm = currentTerm + 1
        _ <- persistentStorage.state(newTerm, nodeId)
        log <- persistentStorage.log
        (lastLogIndex, lastLogTerm) = lastLogIndexAndTerm(log)
        votes <- requestVotes(VoteRequested(newTerm, nodeId, lastLogIndex, lastLogTerm))
      } yield {
          if (hasMajority(newTerm, votes))
            nodeFSM ! MajorityVoteReceived
          else
            ()
        }
    }

    private def hasMajority(term: Int, votes: Seq[RequestVoteResult]): Boolean = {
      val myOwnVote = 1
      votes.count(vote => vote.voteGranted && vote.term == term) + myOwnVote > votes.size / 2
    }

    private def lastLogIndexAndTerm(log: Vector[LogEntry[Command]]): (Int, Int) = {
      log.lastOption.map(lastLogEntry => (lastLogEntry.index, lastLogEntry.term)).getOrElse((-1, -1))
    }

    private def becomeLeader(event: NodeEvent): Future[Unit] = {
      roleState.set(new Leader(this))
    }
  }

  case class CommonVolatileState[Command](commitIndex: Int, lastApplied: Int)

  case class LeaderVolatileState(nextIndex: Seq[Int], matchIndex: Seq[Int])

  sealed trait NodeState[Command] {
    def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult]
    def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult]
  }

  class Follower[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext) extends NodeState[Command] {

    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] = {
      for {
        (currentTerm, _) <- raftNode.persistentStorage.state
        log <- raftNode.persistentStorage.log
        volatileState <- raftNode.commonVolatileState.get
        appendEntriesResult <- applyAppendEntriesRules1To5(entriesToAppend, currentTerm, log, volatileState)
      } yield {
        appendEntriesResult
      }
    }

    // AppendEntries RPC receiver implementation (figure 2), rules 1-5
    private def applyAppendEntriesRules1To5(
        entriesToAppend: EntriesToAppend[Command],
        currentTerm: Int,
        log: Vector[LogEntry[Command]],
        volatileState: CommonVolatileState[Command]): Future[AppendEntriesResult] = {

      if (appendEntriesConsistencyCheck1(entriesToAppend.term, currentTerm)) {

        liftF(AppendEntriesResult(term = currentTerm, success = false))

      } else if (appendEntriesConsistencyCheck2(log, entriesToAppend.prevLogIndex)) {

        liftF(AppendEntriesResult(term = currentTerm, success = false))

      } else {
        val conflicts = appendEntriesConflictSearch(log, entriesToAppend)
        if (conflicts != 0) {

          for {
            _ <- raftNode.persistentStorage.dropRight(conflicts)
          } yield AppendEntriesResult(term = currentTerm, success = false)

        } else {
          val additions: Seq[LogEntry[Command]] = appendEntriesAdditions(log, entriesToAppend)
          val appendFutures = additions.map(addition => raftNode.persistentStorage.log(addition))
          for {
            _ <- Future.sequence(appendFutures)
            newLog <- raftNode.persistentStorage.log
            newState <- appendEntriesCommitIndexCheck(entriesToAppend.leaderCommitIndex,
                                                      iLastNewEntry(additions),
                                                      volatileState)
          } yield AppendEntriesResult(term = currentTerm, success = true)
        }
      }
    }

    private def iLastNewEntry(additions: Seq[LogEntry[Command]]): Int =
      additions.lastOption.map(_.index).getOrElse(Int.MaxValue)

    private def liftF[T](t: => T): Future[T] =
      raftNode.commonVolatileState.get.map(_ => t)

    // AppendEntries summary note #1 (Sec 5.1 consistency check)
    private def appendEntriesConsistencyCheck1(term: Int, currentTerm: Int): Boolean =
      term < currentTerm

    // AppendEntries summary note #2 (Sec 5.3 Consistency check)
    private def appendEntriesConsistencyCheck2(log: Vector[LogEntry[Command]], prevLogIndex: Int): Boolean = {
      @tailrec
      def loop(i: Int): Boolean = {
        if (i == -1)
          true
        else if (log(i).index == prevLogIndex)
          false
        else
          loop(i - 1)
      }
      if (prevLogIndex == -1) // need to handle the case of leader with empty log
        false
      else
        loop(log.size - 1)
    }

    // AppendEntries summary note #3 (Sec 5.3 deleting inconsistent log entries)
    private def appendEntriesConflictSearch(log: Vector[LogEntry[Command]],
                                            entriesToAppend: EntriesToAppend[Command]): Int = {

      val logSz = log.size

      val minEntryIndex: Int = entriesToAppend.entries.headOption.map(head => head.index).getOrElse(logSz)

      @tailrec
      def loop(i: Int, iMin: Int): Int = { // reverse search for the minimum index of a conflicting entry

        if (i == -1)
          iMin
        else {
          val current: LogEntry[Command] = log(i)

          if (minEntryIndex > current.index) { // all entries to append have a higher index than current, terminate reverse search
            iMin
          } else {
            // same index but different terms check
            val maybeConflictingEntry: Option[LogEntry[Command]] =
              entriesToAppend.entries.find(entryToAppend =>
                current.index == entryToAppend.index && current.term != entryToAppend.term)

            if (maybeConflictingEntry.isDefined)
              loop(i - 1, i)
            else
              loop(i - 1, iMin)
          }
        }
      }

      val iMin = loop(logSz - 1, logSz)

      logSz - iMin
    }

    // AppendEntries summary note #4 (append new entries not already in the log)
    private def appendEntriesAdditions(log: Vector[LogEntry[Command]],
                                       entriesToAppend: EntriesToAppend[Command]): Seq[LogEntry[Command]] = {

      val logSz = log.size

      val minEntryIndex: Int = entriesToAppend.entries.headOption.map(head => head.index).getOrElse(logSz)

      // can assume the terms are consistent here.
      // find the entriesToAppend entry without a matching log entry.
      @tailrec
      def loop(i: Int, nDrop: Int): Int = {
        if (i == -1)
          nDrop
        else {
          val current = log(i)

          if (minEntryIndex > current.index)
            nDrop
          else {
            val maybeOverlappingEntry: Option[LogEntry[Command]] =
              entriesToAppend.entries.find(entryToAppend => current.index == entryToAppend.index)

            if (maybeOverlappingEntry.isDefined)
              loop(i - 1, nDrop + 1)
            else
              loop(i - 1, nDrop)

          }
        }
      }
      val nDrop = loop(logSz - 1, 0)
      entriesToAppend.entries.drop(nDrop)
    }

    // AppendEntries summary note #5 (if leaderCommit > commitIndex,
    // set commitIndex = min(leaderCommit, index of last new entry)
    private def appendEntriesCommitIndexCheck(
        leaderCommitIndex: Int,
        iLastNewEntry: Int,
        nodeState: CommonVolatileState[Command]): Future[CommonVolatileState[Command]] = {
      val commitIndex = Math.min(leaderCommitIndex, iLastNewEntry)

      if (leaderCommitIndex > nodeState.commitIndex) {
        for {
          _ <- raftNode.commonVolatileState.set(nodeState.copy(commitIndex = commitIndex))
          newVolatileState <- raftNode.commonVolatileState.get
        } yield newVolatileState
      } else {
        raftNode.commonVolatileState.get
      }
    }


    override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] = ???
  }

  class Candidate[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext) extends NodeState[Command] {
    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] = {
      // rules for servers, candidates
      // if append entries rpc received from new leader, convert to follower
      val prospectiveLeaderTerm = entriesToAppend.term
      for {
        (currentTerm, _) <- raftNode.persistentStorage.state
      } yield {
        if (prospectiveLeaderTerm >= currentTerm) {
          // the term will have been updated via rules for servers note 2.
          raftNode.nodeFSM ! LeaderDiscovered
          AppendEntriesResult(currentTerm, success = true)
        } else {
          AppendEntriesResult(currentTerm, success = false)
        }
      }
    }
    override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] = ???
  }

  class Leader[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext)
      extends NodeState[Command] {

    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] = {
      val prospectiveLeaderTerm = entriesToAppend.term
      for {
        (currentTerm, _) <- raftNode.persistentStorage.state
      } yield {
        if (prospectiveLeaderTerm >= currentTerm) {
          // the term will have been updated via rules for servers note 2.
          raftNode.nodeFSM ! NodeWithHigherTermDiscovered
          AppendEntriesResult(currentTerm, success = true)
        } else {
          AppendEntriesResult(currentTerm, success = false)
        }
      }
    }
    override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] = ???
  }

  case class EntriesToAppend[Command](term: Int,
                                      leaderId: String,
                                      prevLogIndex: Int,
                                      prevLogTerm: Int,
                                      entries: Seq[LogEntry[Command]],
                                      leaderCommitIndex: Int)
  case class AppendEntriesResult(term: Int, success: Boolean)

  case class VoteRequested(term: Int, candidateId: String, lastLogIndex: Int, lastLogTerm: Int)
  case class RequestVoteResult(term: Int, voteGranted: Boolean)


  sealed trait NodeEvent

  case object Start extends NodeEvent
  case object ElectionTimeout extends NodeEvent
  case object MajorityVoteReceived extends NodeEvent
  case object NodeWithHigherTermDiscovered extends NodeEvent
  case object LeaderDiscovered extends NodeEvent

  // Figure 4: Server states.
  class NodeFSM[Command](becomeFollower: NodeEvent => Unit,
                         becomeCandidate: NodeEvent => Unit,
                         becomeLeader: NodeEvent => Unit) {

    private def followerState: Behavior[NodeEvent] = setup { _ =>
      becomeFollower(Start)

      receiveMessage {
        case ElectionTimeout =>
          becomeCandidate(ElectionTimeout)
          candidateState
        case _ =>
          unhandled
      }
    }

    private def candidateState: Behavior[NodeEvent] =
      receiveMessage {
        case ElectionTimeout =>
          becomeCandidate(ElectionTimeout)
          same
        case MajorityVoteReceived =>
          becomeLeader(MajorityVoteReceived)
          leaderState
        case LeaderDiscovered =>
          becomeFollower(LeaderDiscovered)
          followerState
        case _ =>
          unhandled
      }

    private def leaderState: Behavior[NodeEvent] =
      receiveMessage {
        case NodeWithHigherTermDiscovered =>
          becomeFollower(NodeWithHigherTermDiscovered)
          followerState
        case _ =>
          unhandled
      }

    def stateMachine: Behavior[NodeEvent] = followerState
  }
}
