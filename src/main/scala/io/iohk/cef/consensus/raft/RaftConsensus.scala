package io.iohk.cef.consensus.raft

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.stm._

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
  type RPCFactory[Command] =
    (String, EntriesToAppend[Command] => AppendEntriesResult, VoteRequested => RequestVoteResult) => RPC[Command]

  trait RPC[Command] {

    def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult]

    def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult]
  }

  trait PersistentStorage[Command] {
    def state: (Int, String)
    def log: Vector[LogEntry[Command]]
  }

  case class Redirect(leader: String)

  type TimerFactory = (() => Unit) => Timer

  trait Timer {
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
    // todo iterate over cluster members, following redirects until finding one that does not redirect
    def appendEntries(entries: Seq[Command]): Future[Either[Redirect, Unit]] = ???
  }

  class RaftNode[Command](val nodeId: String,
                          val clusterMemberIds: Set[String],
                          rpcFactory: RPCFactory[Command],
                          electionTimerFactory: TimerFactory,
                          heartbeatTimerFactory: TimerFactory,
                          val stateMachine: Command => Unit,
                          persistentStorage: PersistentStorage[Command])(implicit ec: ExecutionContext) {

    private val clusterMembers: Set[RPC[Command]] = clusterMemberIds
      .filterNot(_ == nodeId)
      .map(rpcFactory(_, appendEntries, requestVote))

    val roleState: Ref[NodeState[Command]] = Ref.make[NodeState[Command]]()

    val commonVolatileState: Ref[CommonVolatileState[Command]] = Ref.make[CommonVolatileState[Command]]()

    val leaderVolatileState: Ref[LeaderVolatileState] = Ref.make[LeaderVolatileState]()

    val persistentState: Ref[(Int, String)] = Ref(persistentStorage.state)

    val log: Ref[Vector[LogEntry[Command]]] = Ref(persistentStorage.log)

    val nodeFSM = new NodeFSM(becomeFollower, becomeCandidate, becomeLeader)

    private val electionTimer = electionTimerFactory(() => nodeFSM(ElectionTimeout))

    private val heartbeatTimer = heartbeatTimerFactory(() => sendHeartbeat())

    def getRoleState: NodeState[Command] =
      roleState.single()

    def getPersistentState: (Int, String) =
      persistentState.single()

    def getLog: Vector[LogEntry[Command]] =
      log.single()

    def getCommonVolatileState: CommonVolatileState[Command] =
      commonVolatileState.single()

    def getLeaderVolatileState: LeaderVolatileState =
      leaderVolatileState.single()

    def clientAppendEntries(entries: Seq[Command]): Either[Redirect, Unit] =
      roleState.single().clientAppendEntries(entries)

    def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = atomic { implicit txn =>
      rulesForServersAllServers2(entriesToAppend.term)
      val appendResult = roleState().appendEntries(entriesToAppend)
      applyUncommittedLogEntries()
      appendResult
    }

    def requestVote(voteRequested: VoteRequested): RequestVoteResult = atomic { implicit txn =>
      rulesForServersAllServers2(voteRequested.term)

      val (currentTerm, votedFor) = persistentState()
      val (lastLogIndex, lastLogTerm) = lastLogIndexAndTerm(log())

      if (voteRequested.term < currentTerm) // receiver implementation, note #1
        RequestVoteResult(currentTerm, voteGranted = false)
      else if (
        (votedFor.isEmpty || votedFor == voteRequested.candidateId)
        && ((lastLogTerm <= voteRequested.lastLogTerm) && (lastLogIndex <= voteRequested.lastLogIndex)))
        RequestVoteResult(voteRequested.term, voteGranted = true)
      else
        RequestVoteResult(currentTerm, voteGranted = false)
    }

    def requestVotes(voteRequested: VoteRequested): Future[Seq[RequestVoteResult]] = {
      val rpcFutures = clusterMembers.toSeq.map(memberRpc => memberRpc.requestVote(voteRequested))
      Future.sequence(rpcFutures)
    }

    def applyEntry(iEntry: Int, log: Vector[LogEntry[Command]]): Unit =
      stateMachine(log(iEntry).command)

    // Rules for servers, all servers, note 1.
    // If commitIndex > lastApplied, apply log[lastApplied] to the state machine.
    // (do this recursively until commitIndex == lastApplied)
    private def applyUncommittedLogEntries(): Unit = atomic { implicit txn =>
      val state = commonVolatileState()
      val commitIndex = state.commitIndex
      val lastApplied = state.lastApplied

      if (commitIndex > lastApplied) {
        val nextApplication = lastApplied + 1
        val nextState: CommonVolatileState[Command] = state.copy(lastApplied = nextApplication)
        commonVolatileState() = nextState
        applyEntry(nextApplication, log())
        applyUncommittedLogEntries()
      } else {
        ()
      }
    }

    // Rules for servers (figure 2), all servers, note 2.
    // If request (or response) contains term T > currentTerm
    // set currentTerm = T (convert to follower)
    private def rulesForServersAllServers2(term: Int): Unit =
      atomic { implicit txn =>
        val (currentTerm, votedFor) = persistentState()
        if (term > currentTerm) {
          persistentState() = (term, votedFor)
          nodeFSM(NodeWithHigherTermDiscovered)
        }
      }

    // Note, this is different to the paper which specifies
    // one-based array ops, whereas we use zero-based.
    private def initialState(): CommonVolatileState[Command] = {
      val initialCommitIndex = -1
      val initialLastApplied = -1
      CommonVolatileState(initialCommitIndex, initialLastApplied)
    }

    private def becomeFollower(event: NodeEvent): Unit = atomic { implicit txn =>
      event match {
        case Start =>
          commonVolatileState() = initialState()
          roleState() = new Follower(this)
        case _ =>
          roleState() = new Follower(this)
      }
    }

    // On conversion to candidate, start election:
    // Increment currentTerm
    // Vote for self
    // Reset election timer
    // Send RequestVote RPCs to all other servers
    private def becomeCandidate(event: NodeEvent): Unit = atomic { implicit txn =>
      electionTimer.reset()
      roleState() = new Candidate(this)
      val (currentTerm, _) = persistentState()
      val newTerm = currentTerm + 1
      val (lastLogIndex, lastLogTerm) = lastLogIndexAndTerm(log())
      persistentState() = (newTerm, nodeId)
      requestVotes(VoteRequested(newTerm, nodeId, lastLogIndex, lastLogTerm)).foreach(votes => {
        if (hasMajority(newTerm, votes))
          nodeFSM(MajorityVoteReceived)
      })
    }

    private def hasMajority(term: Int, votes: Seq[RequestVoteResult]): Boolean = {
      val myOwnVote = 1
      votes.count(vote => vote.voteGranted && vote.term == term) + myOwnVote > votes.size / 2
    }

    def lastLogIndexAndTerm(log: Vector[LogEntry[Command]]): (Int, Int) = {
      log.lastOption.map(lastLogEntry => (lastLogEntry.index, lastLogEntry.term)).getOrElse((-1, -1))
    }

    private def becomeLeader(event: NodeEvent): Unit = atomic { implicit txn =>
      roleState() = new Leader(this)
      sendHeartbeat()
    }

    private def sendHeartbeat(): Future[Seq[AppendEntriesResult]] = atomic { implicit txn =>
      val (currentTerm, _) = persistentState()
      val (prevLogIndex, prevLogTerm) = lastLogIndexAndTerm(log())
      val commitIndex = commonVolatileState().commitIndex
      val heartbeat = EntriesToAppend[Command](currentTerm, nodeId, prevLogIndex, prevLogTerm, Seq(), commitIndex)
      val rpcFutures = clusterMembers.toSeq.map(memberRpc => memberRpc.appendEntries(heartbeat))
      Future.sequence(rpcFutures)
    }
  }

  case class CommonVolatileState[Command](commitIndex: Int, lastApplied: Int)

  case class LeaderVolatileState(nextIndex: Seq[Int], matchIndex: Seq[Int])

  sealed trait NodeState[Command] {
    def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult
    def clientAppendEntries(entries: Seq[Command]): Either[Redirect, Unit]
  }

  class Follower[Command](raftNode: RaftNode[Command]) extends NodeState[Command] {

    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult =
      applyAppendEntriesRules1To5(entriesToAppend)

    // AppendEntries RPC receiver implementation (figure 2), rules 1-5
    private def applyAppendEntriesRules1To5(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = atomic {
      implicit txn =>
        val log = raftNode.log()
        val (currentTerm, _) = raftNode.persistentState()
        val volatileState = raftNode.commonVolatileState()

        if (appendEntriesConsistencyCheck1(entriesToAppend.term, currentTerm)) {

          AppendEntriesResult(term = currentTerm, success = false)

        } else if (appendEntriesConsistencyCheck2(log, entriesToAppend.prevLogIndex)) {

          AppendEntriesResult(term = currentTerm, success = false)

        } else {
          val conflicts = appendEntriesConflictSearch(log, entriesToAppend)
          if (conflicts != 0) {
            raftNode.log() = log.dropRight(conflicts)
            AppendEntriesResult(term = currentTerm, success = false)

          } else {
            val additions: Seq[LogEntry[Command]] = appendEntriesAdditions(log, entriesToAppend)
            raftNode.log() = log ++ additions
            appendEntriesCommitIndexCheck(entriesToAppend.leaderCommitIndex, iLastNewEntry(additions), volatileState)
            AppendEntriesResult(term = currentTerm, success = true)
          }
        }
    }

    private def iLastNewEntry(additions: Seq[LogEntry[Command]]): Int =
      additions.lastOption.map(_.index).getOrElse(Int.MaxValue)

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
    private def appendEntriesCommitIndexCheck(leaderCommitIndex: Int,
                                              iLastNewEntry: Int,
                                              nodeState: CommonVolatileState[Command]): Unit = {
      val commitIndex = Math.min(leaderCommitIndex, iLastNewEntry)
      atomic { implicit txn =>
        if (leaderCommitIndex > nodeState.commitIndex) {
          raftNode.commonVolatileState() = nodeState.copy(commitIndex = commitIndex)
        }
      }
    }
    override def clientAppendEntries(entries: Seq[Command]): Either[Redirect, Unit] = {
      val (_, votedFor) = raftNode.persistentState.single()
      Left(Redirect(votedFor))
    }
  }

  class Candidate[Command](raftNode: RaftNode[Command]) extends NodeState[Command] {
    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = atomic {
      implicit txn =>
        // rules for servers, candidates
        // if append entries rpc received from new leader, convert to follower
        val prospectiveLeaderTerm = entriesToAppend.term
        val (currentTerm, _) = raftNode.persistentState()
        if (prospectiveLeaderTerm >= currentTerm) {
          // the term will have been updated via rules for servers note 2.
          raftNode.nodeFSM(LeaderDiscovered)
          AppendEntriesResult(currentTerm, success = true)
        } else {
          AppendEntriesResult(currentTerm, success = false)
        }
    }
    override def clientAppendEntries(entries: Seq[Command]): Either[Redirect, Unit] = {
      val (_, votedFor) = raftNode.persistentState.single()
      Left(Redirect(votedFor))
    }
  }

  class Leader[Command](raftNode: RaftNode[Command]) extends NodeState[Command] {

    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = atomic {
      implicit txn =>
        val prospectiveLeaderTerm = entriesToAppend.term
        val (currentTerm, _) = raftNode.persistentState()
        if (prospectiveLeaderTerm >= currentTerm) {
          // the term will have been updated via rules for servers note 2.
          raftNode.nodeFSM(NodeWithHigherTermDiscovered)
          AppendEntriesResult(currentTerm, success = true)
        } else {
          AppendEntriesResult(currentTerm, success = false)
        }
    }
    override def clientAppendEntries(entries: Seq[Command]): Either[Redirect, Unit] = atomic { implicit txn =>
      val log = raftNode.log()
      val (lastLogIndex, _) = raftNode.lastLogIndexAndTerm(log)
      val (currentTerm, _) = raftNode.persistentState()

      val (_, entriesToAppend) = entries.foldLeft((lastLogIndex, Vector[LogEntry[Command]]()))((acc, nextCommand) => {
        val (logIndex, entriesToAppend) = acc
        val nextLogIndex = logIndex + 1
        val nextEntry = LogEntry[Command](nextCommand, currentTerm, nextLogIndex)
        (nextLogIndex, entriesToAppend :+ nextEntry)
      })

      raftNode.log() = log ++ entriesToAppend

      entries.foreach(entry => raftNode.stateMachine.apply(entry))

      Right(())
    }
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
  class NodeFSM(becomeFollower: NodeEvent => Unit,
                becomeCandidate: NodeEvent => Unit,
                becomeLeader: NodeEvent => Unit) {

    trait State[T] {
      def apply(event: T): State[T]
    }

    private val followerState = new FollowerState
    private val candidateState = new CandidateState
    private val leaderState = new LeaderState
    private val state: Ref[State[NodeEvent]] = Ref(followerState)

    def apply(event: NodeEvent): Unit = atomic { implicit txn =>
      state() = state().apply(event)
    }

    class FollowerState extends State[NodeEvent] {
      becomeFollower(Start)
      override def apply(event: NodeEvent): State[NodeEvent] = event match {
        case ElectionTimeout =>
          becomeCandidate(ElectionTimeout)
          candidateState
        case _ =>
          followerState
      }
    }

    class CandidateState extends State[NodeEvent] {
      override def apply(event: NodeEvent): State[NodeEvent] = event match {
        case ElectionTimeout =>
          becomeCandidate(ElectionTimeout)
          candidateState
        case MajorityVoteReceived =>
          becomeLeader(MajorityVoteReceived)
          leaderState
        case LeaderDiscovered =>
          becomeFollower(LeaderDiscovered)
          followerState
        case _ =>
          candidateState
      }
    }

    class LeaderState extends State[NodeEvent] {
      override def apply(event: NodeEvent): State[NodeEvent] = event match {
        case NodeWithHigherTermDiscovered =>
          becomeFollower(NodeWithHigherTermDiscovered)
          followerState
        case _ =>
          leaderState
      }
    }
  }
}
