package io.iohk.cef.consensus.raft

import scala.annotation.tailrec
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
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

    def clientAppendEntries(entries: Seq[Command]): Future[Either[Redirect[Command], Unit]]
  }

  trait PersistentStorage[Command] {
    def state: (Int, String)
    def log: Vector[LogEntry[Command]]
  }

  case class Redirect[Command](leaderRPC: RPC[Command])

  type RaftTimerFactory = (() => Unit) => RaftTimer

  trait RaftTimer {
    def reset(): Unit
  }

  /**
    * From Figure 1.
    * TODO new entries must go via the leader, so the module will need
    * to talk to the correct node.
    */
  class RaftConsensus[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext) {
    // Sec 5.1
    // The leader handles all client requests (if
    // a client contacts a follower, the follower redirects it to the leader)
    def appendEntries(entries: Seq[Command]): Future[Unit] = {
      appendEntries(raftNode.getLeader, entries)
    }

    private def appendEntries(leaderRpc: RPC[Command], entries: Seq[Command]): Future[Unit] = {
      leaderRpc.clientAppendEntries(entries).flatMap {
        case Left(Redirect(nextLeaderRPC)) =>
          appendEntries(nextLeaderRPC, entries)
        case Right(()) =>
          Future(())
      }
    }
  }

  case class RaftContext[Command](roleState: NodeState[Command],
                                  commonVolatileState: CommonVolatileState[Command],
                                  leaderVolatileState: LeaderVolatileState,
                                  persistentState: (Int, String),
                                  log: Vector[LogEntry[Command]],
                                  leaderId: String,
                                  nodeFSM: NodeFSM)

  class RaftNode[Command](val nodeId: String,
                          val clusterMemberIds: Seq[String],
                          rpcFactory: RPCFactory[Command],
                          electionTimerFactory: RaftTimerFactory,
                          heartbeatTimerFactory: RaftTimerFactory,
                          val stateMachine: Command => Unit,
                          persistentStorage: PersistentStorage[Command])(implicit ec: ExecutionContext) {

    val clusterMembers: Seq[RPC[Command]] = clusterMemberIds
      .filterNot(_ == nodeId)
      .map(rpcFactory(_, appendEntries, requestVote))

    val clusterTable: Map[String, RPC[Command]] = clusterMemberIds
      .filterNot(_ == nodeId)
      .map(peerId => peerId -> rpcFactory(peerId, appendEntries, requestVote)).toMap

    val rc: Ref[RaftContext[Command]] = Ref(initialRaftContext())

    val electionTimer = electionTimerFactory(() => getNodeFSM(ElectionTimeout))

    val heartbeatTimer = heartbeatTimerFactory(() => sendHeartbeat())

    def getNodeFSM: NodeFSM =
      rc.single().nodeFSM

    def getLeader: RPC[Command] =
      clusterTable(rc.single().leaderId)

    def getRoleState: NodeState[Command] =
      rc.single().roleState

    def getPersistentState: (Int, String) =
      rc.single().persistentState

    def getLog: Vector[LogEntry[Command]] =
      rc.single().log

    def getCommonVolatileState: CommonVolatileState[Command] =
      rc.single().commonVolatileState

    def getLeaderVolatileState: LeaderVolatileState =
      rc.single().leaderVolatileState

    def clientAppendEntries(entries: Seq[Command]): Either[Redirect[Command], Unit] =
      rc.single().roleState.clientAppendEntries(entries)

    def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = atomic { implicit txn =>
      rulesForServersAllServers2(entriesToAppend.term)
      val appendResult = rc().roleState.appendEntries(entriesToAppend)
      applyUncommittedLogEntries()
      appendResult
    }

    def requestVote(voteRequested: VoteRequested): RequestVoteResult = atomic { implicit txn =>
      rulesForServersAllServers2(voteRequested.term)

      val (currentTerm, votedFor) = rc().persistentState
      val (lastLogIndex, lastLogTerm) = lastLogIndexAndTerm(rc().log)

      if (voteRequested.term < currentTerm) // receiver implementation, note #1
        RequestVoteResult(currentTerm, voteGranted = false)
      else if ((votedFor.isEmpty || votedFor == voteRequested.candidateId)
               && ((lastLogTerm <= voteRequested.lastLogTerm) && (lastLogIndex <= voteRequested.lastLogIndex)))
        RequestVoteResult(voteRequested.term, voteGranted = true)
      else
        RequestVoteResult(currentTerm, voteGranted = false)
    }

    def requestVotes(voteRequested: VoteRequested): Seq[RequestVoteResult] = {
      val rpcFutures = clusterMembers.map(memberRpc => memberRpc.requestVote(voteRequested))
      Await.result(Future.sequence(rpcFutures), 5 seconds)
    }

    def applyEntry(iEntry: Int, log: Vector[LogEntry[Command]]): Unit =
      stateMachine(log(iEntry).command)

    // Rules for servers, all servers, note 1.
    // If commitIndex > lastApplied, apply log[lastApplied] to the state machine.
    // (do this recursively until commitIndex == lastApplied)
    def applyUncommittedLogEntries(): Unit = atomic { implicit txn =>
      val theLog = rc().log
      @tailrec
      def loop(state: CommonVolatileState[Command]): CommonVolatileState[Command] = {
        if (state.commitIndex > state.lastApplied) {
          val nextApplication = state.lastApplied + 1
          applyEntry(nextApplication, theLog)
          loop(state.copy(lastApplied = nextApplication))
        } else {
          state
        }
      }
      val commonVolatileState = rc().commonVolatileState
      val nextState = loop(commonVolatileState)
      rc() = rc().copy(commonVolatileState = nextState)
    }

    // Rules for servers (figure 2), all servers, note 2.
    // If request (or response) contains term T > currentTerm
    // set currentTerm = T (convert to follower)
    private def rulesForServersAllServers2(term: Int): Unit =
      atomic { implicit txn =>
        val (currentTerm, votedFor) = rc().persistentState
        if (term > currentTerm) {
          rc() = rc().copy(persistentState = (term, votedFor))
          rc().nodeFSM(NodeWithHigherTermDiscovered)
        }
      }

    // Note, this is different to the paper which specifies
    // one-based array ops, whereas we use zero-based.
    private def initialCommonState(): CommonVolatileState[Command] = {
      val initialCommitIndex = -1
      val initialLastApplied = -1
      CommonVolatileState(initialCommitIndex, initialLastApplied)
    }

    private def initialLeaderState(log: Vector[LogEntry[Command]]): LeaderVolatileState = {
      val (lastLogIndex, _) = lastLogIndexAndTerm(log)
      val nextIndex = Seq.fill(clusterMembers.size)(lastLogIndex + 1)
      val matchIndex = Seq.fill(clusterMembers.size)(-1)
      LeaderVolatileState(nextIndex, matchIndex)
    }

    private def initialRaftContext(): RaftContext[Command] = {
      val (currentTerm, votedFor) = persistentStorage.state
      val log = persistentStorage.log
      RaftContext(new Follower(this),
        initialCommonState(),
        initialLeaderState(log),
        (currentTerm, votedFor),
        log,
        votedFor,
        new NodeFSM(becomeFollower, becomeCandidate, becomeLeader))
    }

    private def becomeFollower(event: NodeEvent): Unit = atomic { implicit txn =>
      rc() = rc().copy(roleState = new Follower(this))
    }

    // On conversion to candidate, start election:
    // Increment currentTerm
    // Vote for self
    // Reset election timer
    // Send RequestVote RPCs to all other servers
    private def becomeCandidate(event: NodeEvent): Unit = atomic { implicit txn =>
      electionTimer.reset()
      rc() = rc().copy(roleState = new Candidate(this))
      val (currentTerm, _) = rc().persistentState
      val newTerm = currentTerm + 1
      val (lastLogIndex, lastLogTerm) = lastLogIndexAndTerm(rc().log)
      rc() = rc().copy(persistentState = (newTerm, nodeId))
      val votes = requestVotes(VoteRequested(newTerm, nodeId, lastLogIndex, lastLogTerm))
      if (hasMajority(newTerm, votes))
        rc().nodeFSM(MajorityVoteReceived)
    }

    private def hasMajority(term: Int, votes: Seq[RequestVoteResult]): Boolean = {
      val myOwnVote = 1
      votes.count(vote => vote.voteGranted && vote.term == term) + myOwnVote > (votes.size + myOwnVote) / 2
    }

    def lastLogIndexAndTerm(log: Vector[LogEntry[Command]]): (Int, Int) = {
      log.lastOption.map(lastLogEntry => (lastLogEntry.index, lastLogEntry.term)).getOrElse((-1, -1))
    }

    private def becomeLeader(event: NodeEvent): Unit = atomic { implicit txn =>
      rc() = rc().copy(roleState = new Leader(this))
      sendHeartbeat()
    }

    private def sendHeartbeat(): Unit = atomic { implicit txn =>
      rc().roleState.sendHeartbeat()
    }
  }

  case class CommonVolatileState[Command](commitIndex: Int, lastApplied: Int)

  case class LeaderVolatileState(nextIndex: Seq[Int], matchIndex: Seq[Int])

  sealed trait NodeState[Command] {
    def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult
    def sendHeartbeat(): Unit
    def clientAppendEntries(entries: Seq[Command]): Either[Redirect[Command], Unit]
  }

  class Follower[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext) extends NodeState[Command] {

    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = {
      applyAppendEntriesRules1To5(entriesToAppend)
    }

    // AppendEntries RPC receiver implementation (figure 2), rules 1-5
    private def applyAppendEntriesRules1To5(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = atomic {
      implicit txn =>
        raftNode.rc() = raftNode.rc().copy(leaderId = entriesToAppend.leaderId)
        val log = raftNode.rc().log
        val (currentTerm, _) = raftNode.rc().persistentState
        val volatileState = raftNode.rc().commonVolatileState

        if (appendEntriesConsistencyCheck1(entriesToAppend.term, currentTerm)) {

          AppendEntriesResult(term = currentTerm, success = false)

        } else if (appendEntriesConsistencyCheck2(log, entriesToAppend.prevLogIndex)) {

          AppendEntriesResult(term = currentTerm, success = false)

        } else {
          val conflicts = appendEntriesConflictSearch(log, entriesToAppend)
          if (conflicts != 0) {
            raftNode.rc() = raftNode.rc().copy(log = log.dropRight(conflicts))
            AppendEntriesResult(term = currentTerm, success = false)
          } else {
            val additions: Seq[LogEntry[Command]] = appendEntriesAdditions(log, entriesToAppend)
            raftNode.rc() = raftNode.rc().copy(log = log ++ additions)
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
          raftNode.rc() = raftNode.rc().copy(commonVolatileState = nodeState.copy(commitIndex = commitIndex))
        }
      }
    }
    override def clientAppendEntries(entries: Seq[Command]): Either[Redirect[Command], Unit] = {
      Left(Redirect(raftNode.getLeader))
    }
    override def sendHeartbeat(): Unit = ()
  }

  class Candidate[Command](raftNode: RaftNode[Command]) extends NodeState[Command] {
    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = atomic {
      implicit txn =>
        // rules for servers, candidates
        // if append entries rpc received from new leader, convert to follower
        val prospectiveLeaderTerm = entriesToAppend.term
        val (currentTerm, _) = raftNode.rc().persistentState
        if (prospectiveLeaderTerm >= currentTerm) {
          // the term will have been updated via rules for servers note 2.
          raftNode.rc().nodeFSM(LeaderDiscovered)
          AppendEntriesResult(currentTerm, success = true)
        } else {
          AppendEntriesResult(currentTerm, success = false)
        }
    }
    override def clientAppendEntries(entries: Seq[Command]): Either[Redirect[Command], Unit] =
      Left(Redirect(raftNode.getLeader))

    override def sendHeartbeat(): Unit = ()
  }

  class Leader[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext) extends NodeState[Command] {

    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = atomic {
      implicit txn =>
        val prospectiveLeaderTerm = entriesToAppend.term
        val (currentTerm, _) = raftNode.rc().persistentState
        if (prospectiveLeaderTerm >= currentTerm) {
          // the term will have been updated via rules for servers note 2.
          raftNode.rc().nodeFSM(NodeWithHigherTermDiscovered)
          AppendEntriesResult(currentTerm, success = true)
        } else {
          AppendEntriesResult(currentTerm, success = false)
        }
    }

    override def clientAppendEntries(entries: Seq[Command]): Either[Redirect[Command], Unit] = atomic { implicit txn =>
      val log = raftNode.rc().log
      val (lastLogIndex, _) = raftNode.lastLogIndexAndTerm(log)
      val (currentTerm, _) = raftNode.rc().persistentState

      val (_, entriesToAppend) = entries.foldLeft((lastLogIndex, Vector[LogEntry[Command]]()))((acc, nextCommand) => {
        val (logIndex, entriesToAppend) = acc
        val nextLogIndex = logIndex + 1
        val nextEntry = LogEntry[Command](nextCommand, currentTerm, nextLogIndex)
        (nextLogIndex, entriesToAppend :+ nextEntry)
      })

      raftNode.rc() = raftNode.rc().copy(log = log ++ entriesToAppend)

      sendAppendEntries()

      Right(())
    }

    override def sendHeartbeat(): Unit = atomic { implicit txn =>
      val heartbeat: EntriesToAppend[Command] = getHeartbeat
      Await.result(Future.sequence(raftNode.clusterMembers.map(memberRpc => memberRpc.appendEntries(heartbeat))), 5 seconds)
    }

    private def sendAppendEntries(): Unit = atomic { implicit txn =>
      val nextIndexes = raftNode.rc().leaderVolatileState.nextIndex
      val (currentTerm, _) = raftNode.rc().persistentState
      val commitIndex = raftNode.rc().commonVolatileState.commitIndex
      val theLog = raftNode.rc().log
      val (lastLogIndex, _) = raftNode.lastLogIndexAndTerm(theLog)

      val callFs: Seq[Future[(Int, Int)]] = raftNode.clusterMembers.indices.map(i =>
        sendAppendEntry(theLog, nextIndexes(i), lastLogIndex, raftNode.clusterMembers(i)))

      val (nextIndex, matchIndex) = Await.result(Future
        .sequence(callFs)
        .map((indices: Seq[(Int, Int)]) => indices.unzip), 1 second)

      // Leaders, final note (#4)
      val maybeN = findN(theLog, currentTerm, commitIndex, matchIndex)

      val nextCommonState =
        maybeN.fold(raftNode.rc().commonVolatileState)(n => raftNode.rc().commonVolatileState.copy(commitIndex = n))

      val nextLeaderState = LeaderVolatileState(nextIndex, matchIndex)

      raftNode.rc() = raftNode.rc().copy(commonVolatileState = nextCommonState)
      raftNode.rc() = raftNode.rc().copy(leaderVolatileState = nextLeaderState)

      raftNode.applyUncommittedLogEntries()

    }

    private def sendAppendEntry(log: Vector[LogEntry[Command]],
                                nextIndex: Int,
                                lastLogIndex: Int,
                                memberRPC: RPC[Command]): Future[(Int, Int)] = atomic { implicit txn =>
      val commitIndex = raftNode.rc().commonVolatileState.commitIndex

      val (currentTerm, _) = raftNode.rc().persistentState
      val appendResultF =
        appendEntryForNode(nextIndex, commitIndex, log, currentTerm, lastLogIndex, memberRPC, getHeartbeat)

      appendResultF.flatMap(appendResult => {
        if (appendResult.success) {
          // Leaders, If successful, update nextIndex and matchIndex for follower.
          Future((lastLogIndex + 1, lastLogIndex))
        } else {
          // Leaders, if fails because of log inconsistency, retry.
          sendAppendEntry(log, nextIndex - 1, lastLogIndex, memberRPC)
        }
      })
    }

    private def appendEntryForNode(nextIndex: Int,
                                   commitIndex: Int,
                                   theLog: Vector[LogEntry[Command]],
                                   currentTerm: Int,
                                   lastLogIndex: Int,
                                   peerRpc: RPC[Command],
                                   heartbeat: EntriesToAppend[Command]): Future[AppendEntriesResult] = {

      if (lastLogIndex >= nextIndex) { // Rules for servers, leaders, note #3.
        val entries = theLog.slice(nextIndex, theLog.size) // NB: will break after impl of log compaction
        val (prevLogIndex, prevLogTerm) =
          if (nextIndex == 0)
            (-1, -1)
          else {
            val previousEntry = theLog(nextIndex - 1)
            (previousEntry.index, previousEntry.term)
          }
        val entriesToAppend =
          EntriesToAppend(currentTerm, raftNode.nodeId, prevLogIndex, prevLogTerm, entries, commitIndex)
        peerRpc.appendEntries(entriesToAppend)
      } else {
        peerRpc.appendEntries(heartbeat)
      }
    }

    private def findN(log: Vector[LogEntry[Command]],
                      currentTerm: Int,
                      commitIndex: Int,
                      matchIndex: Seq[Int]): Option[Int] = {
      @tailrec
      def loop(successN: List[Int], tryN: Int): List[Int] = {
        if (log.size > tryN) {
          val matchingTermN = log(tryN).term == currentTerm

          val majorityN = (matchIndex.count(m => m >= tryN) + 1) > (matchIndex.size + 1) / 2

          if (matchingTermN && majorityN)
            loop(tryN :: successN, tryN + 1)
          else
            loop(successN, tryN + 1)
        } else {
          successN
        }
      }

      val ns = loop(List(), commitIndex + 1)
      if (ns.isEmpty)
        None
      else
        Some(ns.max)
    }
    private def getHeartbeat: EntriesToAppend[Command] = atomic { implicit txn =>
      val (currentTerm, _) = raftNode.rc().persistentState
      val (prevLogIndex, prevLogTerm) = raftNode.lastLogIndexAndTerm(raftNode.rc().log)
      val commitIndex = raftNode.rc().commonVolatileState.commitIndex
      EntriesToAppend[Command](currentTerm, raftNode.nodeId, prevLogIndex, prevLogTerm, Seq(), commitIndex)
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

  type Action = NodeEvent => Unit

  // Figure 4: Server states.
  class NodeFSM(becomeFollower: Action,
                becomeCandidate: Action,
                becomeLeader: Action) {


    trait State[T] {
      def apply(event: T): (State[T], Action)
    }

    private val followerState = new FollowerState
    private val candidateState = new CandidateState
    private val leaderState = new LeaderState
    private val state: Ref[State[NodeEvent]] = Ref(followerState)

    def apply(event: NodeEvent): Unit = atomic { implicit txn =>
      val (nextState, actions) = state().apply(event)
      state() = nextState
      actions.apply(event)
    }

    class FollowerState extends State[NodeEvent] {
      override def apply(event: NodeEvent): (State[NodeEvent], Action) = event match {
        case ElectionTimeout =>
          (candidateState, becomeCandidate)
        case _ =>
          (followerState, _ => ())
      }
    }

    class CandidateState extends State[NodeEvent] {
      override def apply(event: NodeEvent): (State[NodeEvent], Action) = event match {
        case ElectionTimeout =>
          (candidateState, becomeCandidate)
        case MajorityVoteReceived =>
          (leaderState, becomeLeader)
        case LeaderDiscovered =>
          (followerState, becomeFollower)
        case _ =>
          (candidateState, _ => ())
      }
    }

    class LeaderState extends State[NodeEvent] {
      override def apply(event: NodeEvent): (State[NodeEvent], Action) = event match {
        case NodeWithHigherTermDiscovered =>
          (followerState, becomeFollower)
        case _ =>
          (leaderState, _ => ())
      }
    }
  }
}
