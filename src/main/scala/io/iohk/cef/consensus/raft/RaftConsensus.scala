package io.iohk.cef.consensus.raft

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.stm._

object RaftConsensus {

  case class LogEntry[Command](command: Command, term: Int, index: Int)

  /**
    * To be implemented by users of the module to provide their desired RPC implementation.
    * RPCFactory provides the mechanism by which the RaftNode on one host
    * can create RPC instances to talk to the other nodes in the cluster.
    */
  trait RPCFactory[Command] {

    /**
      * @param nodeId This is the nodeId of a node in the raft cluster.
      * @param appendEntriesCallback when a (remote) raft node makes an inbound appendEntries call,
      *                              implementers should invoke this function.
      * @param requestVoteCallback when a (remote) raft node makes an inbound requestVote call,
      *                            implementers should invoke this function.
      * @return
      */
    def apply(nodeId: String,
              appendEntriesCallback: EntriesToAppend[Command] => AppendEntriesResult,
              requestVoteCallback: VoteRequested => RequestVoteResult): RPC[Command]
  }

  /**
    * To be implemented by users of the module using whatever network technology suits them.
    *
    * @tparam Command the user command type.
    */
  trait RPC[Command] {

    /**
      * Make an appendEntries RPC call to a raft node.
      *
      * @param entriesToAppend as described in Figure 2, AppendEntries RPC.
      * @return the result from the raft node.
      */
    def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult]

    /**
      * Make a requestVote RPC call to a raft node.
      *
      * @param voteRequested as described in Figure 2, AppendEntries RPC.
      * @return the result from the raft node.
      */
    def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult]

    /**
      * This function enables the consensus module to support requests from clients
      * as shown in Figure 1.
      *
      * @param entries the commands which the user wishes to append to the log.
      * @return the response from the raft node.
      */
    def clientAppendEntries(entries: Seq[Command]): Future[Either[Redirect[Command], Unit]]
  }

  /**
    * To be implemented by users of the module using whatever persistence technology suits them.
    *
    * @tparam Command the user command type.
    */
  trait PersistentStorage[Command] {
    def state: (Int, String)
    def log: Vector[LogEntry[Command]]
  }

  case class Redirect[Command](leaderRPC: RPC[Command])

  /**
    * Can be implemented by users of the module if they wish to provide a timer implementation.
    */
  trait RaftTimerFactory {

    /**
      * @param timeoutHandler this enables the Raft node to pass a function which the timer should
      *                       call upon timeout.
      * @return a RaftTimer implementation.
      */
    def apply(timeoutHandler: () => Unit): RaftTimer
  }

  /**
    * A RaftTimer should implement a randomized, always on timer.
    * That is,
    * it should schedule a random timeout.
    * when that timeout occurs it should invoke the timeout handler
    * it should then reschedule another random timeout.
    */
  trait RaftTimer {

    /**
      * Reset causes the timer to cancel the current timeout and reschedule
      * another one (without calling the timeoutHandler).
      */
    def reset(): Unit
  }

  /**
    * From Figure 1. The Consensus module ensures client requests go to the leader.
    */
  class RaftConsensus[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext) {
    // Sec 5.1
    // The leader handles all client requests
    // (if a client contacts a follower, the follower redirects it to the leader)
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

  case class RaftContext[Command](role: NodeRole[Command],
                                  commonVolatileState: CommonVolatileState[Command],
                                  leaderVolatileState: LeaderVolatileState,
                                  persistentState: (Int, String),
                                  log: Vector[LogEntry[Command]],
                                  leaderId: String)

  class RaftNode[Command](val nodeId: String,
                          val clusterMemberIds: Seq[String],
                          rpcFactory: RPCFactory[Command],
                          electionTimerFactory: RaftTimerFactory,
                          heartbeatTimerFactory: RaftTimerFactory,
                          val stateMachine: Command => Unit,
                          persistentStorage: PersistentStorage[Command])(implicit ec: ExecutionContext) {

    val clusterTable: Map[String, RPC[Command]] = clusterMemberIds
      .filterNot(_ == nodeId)
      .map(peerId => peerId -> rpcFactory(peerId, appendEntries, requestVote))
      .toMap

    val clusterMembers: Seq[RPC[Command]] = clusterTable.values.toSeq

    private val rc: Ref[RaftContext[Command]] = Ref(initialRaftContext())

    val electionTimer = electionTimerFactory(() => electionTimeout())

    val heartbeatTimer = heartbeatTimerFactory(() => heartbeatTimeout())

    val nodeFSM = new RaftFSM[Command](becomeFollower, becomeCandidate, becomeLeader)

    def getLeader: RPC[Command] = {
      val ctx = rc.single()
      if (ctx.leaderId.nonEmpty)
        clusterTable(ctx.leaderId)
      else {
        val (_, votedFor) = ctx.persistentState
        clusterTable(votedFor)
      }
    }

    def getRole: StateCode =
      rc.single().role.stateCode

    def getPersistentState: (Int, String) =
      rc.single().persistentState

    def getLog: Vector[LogEntry[Command]] =
      rc.single().log

    def getCommonVolatileState: CommonVolatileState[Command] =
      rc.single().commonVolatileState

    def getLeaderVolatileState: LeaderVolatileState =
      rc.single().leaderVolatileState

    // transaction entry points
    // appendEntries       (called from rpc inbound) (not async)
    // requestVote         (called from rpc inbound) (not async)
    // heartbeatTimeout    (called from a timer)     (async)
    // clientAppendEntries (called from externally)  (async)
    // electionTimeout     (called from a timer)     (async)

    // this function is used by pure, synchronous entry points to atomically update the node state.
    private def withRaftContext[T](f: RaftContext[Command] => (RaftContext[Command], T)): T = {
      val initialContext = rc.single()
      val (nextContext, result) = f(initialContext)
      atomic { implicit txn =>
        rc() = nextContext
        result
      }
    }

    // this function is used by asynchronous entry points (the ones that have to contact other nodes) to atomically update the node state.
    private def withFutureRaftContext[T](f: RaftContext[Command] => Future[(RaftContext[Command], T)]): Future[T] = {
      f(rc.single()).map(t =>
        atomic { implicit txn =>
          val (nextContext, result) = t
          rc() = nextContext // NB: on a different thread! This is not the same STM state as the enclosing block.
          result
      })
    }

    def clientAppendEntries(entries: Seq[Command]): Future[Either[Redirect[Command], Unit]] = withFutureRaftContext {
      rc => {
        rc.role.clientAppendEntries(rc, entries).map {
          case (ctx, response) =>
            (applyUncommittedLogEntries(ctx), response)
        }
      }
    }

    // Handler for inbound appendEntries RPCs from leaders.
    def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = withRaftContext { rc =>
      val rc2 = rulesForServersAllServers2(rc, entriesToAppend.term)
      val (rc3, appendResult) = rc2.role.appendEntries(rc2, entriesToAppend)
      val rc4 = applyUncommittedLogEntries(rc3)
      (rc4, appendResult)
    }

    // Handler for inbound requestVote RPCs from candidates.
    def requestVote(voteRequested: VoteRequested): RequestVoteResult = withRaftContext { rc =>
      val rc2 = rulesForServersAllServers2(rc, voteRequested.term)
      val requestVoteResult: RequestVoteResult = getVoteResult(rc2, voteRequested)
      (rc2, requestVoteResult)
    }

    private def electionTimeout(): Unit = {
      withRaftContext(rc => (nodeFSM.apply(rc, ElectionTimeout), ()))
      withFutureRaftContext(rc => requestVotes(rc))
    }

    private def heartbeatTimeout(): Unit =
      withFutureRaftContext(rc => sendHeartbeat(rc))

    private def sendHeartbeat(rc: RaftContext[Command]): Future[(RaftContext[Command], Unit)] = {
      rc.role.clientAppendEntries(rc, Seq()).map { case (ctx, _) => (ctx, ()) }
    }

    private def getVoteResult(rc: RaftContext[Command], voteRequested: VoteRequested): RequestVoteResult = {

      val (currentTerm, votedFor) = rc.persistentState
      val (lastLogIndex, lastLogTerm) = lastLogIndexAndTerm(rc.log)

      if (voteRequested.term < currentTerm) // receiver implementation, note #1
        RequestVoteResult(currentTerm, voteGranted = false)
      else if ((votedFor.isEmpty || votedFor == voteRequested.candidateId)
               && ((lastLogTerm <= voteRequested.lastLogTerm) && (lastLogIndex <= voteRequested.lastLogIndex)))
        RequestVoteResult(voteRequested.term, voteGranted = true)
      else
        RequestVoteResult(currentTerm, voteGranted = false)
    }

    private def requestVotes(voteRequested: VoteRequested): Future[Seq[RequestVoteResult]] = {
      val rpcFutures = clusterMembers.map(memberRpc => memberRpc.requestVote(voteRequested))
      Future.sequence(rpcFutures)
    }

    private def requestVotes(rc: RaftContext[Command]): Future[(RaftContext[Command], Unit)] = {
      val (term, _) = rc.persistentState
      val (lastLogIndex, lastLogTerm) = lastLogIndexAndTerm(rc.log)
      requestVotes(VoteRequested(term, nodeId, lastLogIndex, lastLogTerm)).flatMap(
        votes =>
          if (hasMajority(term, votes)) {
            val rc2 = nodeFSM(rc, MajorityVoteReceived)
            sendHeartbeat(rc2)
          }
          else
            Future((rc, ()))
      )
    }

    private def applyEntry(iEntry: Int, log: Vector[LogEntry[Command]]): Unit =
      stateMachine(log(iEntry).command)

    // Rules for servers, all servers, note 1.
    // If commitIndex > lastApplied, apply log[lastApplied] to the state machine.
    // (do this recursively until commitIndex == lastApplied)
    def applyUncommittedLogEntries(rc: RaftContext[Command]): RaftContext[Command] = {

      @tailrec
      def loop(state: CommonVolatileState[Command]): CommonVolatileState[Command] = {
        if (state.commitIndex > state.lastApplied) {
          val nextApplication = state.lastApplied + 1
          applyEntry(nextApplication, rc.log)
          loop(state.copy(lastApplied = nextApplication))
        } else {
          state
        }
      }
      val commonVolatileState = rc.commonVolatileState
      val nextState = loop(commonVolatileState)
      rc.copy(commonVolatileState = nextState)
    }

    // Rules for servers (figure 2), all servers, note 2.
    // If request (or response) contains term T > currentTerm
    // set currentTerm = T (convert to follower)
    private def rulesForServersAllServers2(rc: RaftContext[Command], term: Int): RaftContext[Command] = {
      val (currentTerm, votedFor) = rc.persistentState
      if (term > currentTerm) {
        nodeFSM(rc.copy(persistentState = (term, votedFor)), NodeWithHigherTermDiscovered)
      } else {
        rc
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
      RaftContext(
        new Follower(this),
        initialCommonState(),
        initialLeaderState(log),
        (currentTerm, votedFor),
        log,
        votedFor
      )
    }

    private def becomeFollower(rc: RaftContext[Command], event: NodeEvent): RaftContext[Command] =
      rc.copy(role = new Follower(this))

    // On conversion to candidate, start election:
    // Increment currentTerm
    // Vote for self
    // Reset election timer
    // Send RequestVote RPCs to all other servers
    private def becomeCandidate(rc: RaftContext[Command], event: NodeEvent): RaftContext[Command] = {
      electionTimer.reset()
      val (currentTerm, _) = rc.persistentState
      val newTerm = currentTerm + 1
      val nextPersistentState = (newTerm, nodeId)
      rc.copy(role = new Candidate(this), persistentState = nextPersistentState)
    }

    private def hasMajority(term: Int, votes: Seq[RequestVoteResult]): Boolean = {
      val myOwnVote = 1
      votes.count(vote => vote.voteGranted && vote.term == term) + myOwnVote > (votes.size + myOwnVote) / 2
    }

    def lastLogIndexAndTerm(log: Vector[LogEntry[Command]]): (Int, Int) = {
      log.lastOption.map(lastLogEntry => (lastLogEntry.index, lastLogEntry.term)).getOrElse((-1, -1))
    }

    private def becomeLeader(rc: RaftContext[Command], event: NodeEvent): RaftContext[Command] = {
      rc.copy(role = new Leader(this))
    }
  }

  case class CommonVolatileState[Command](commitIndex: Int, lastApplied: Int)

  case class LeaderVolatileState(nextIndex: Seq[Int], matchIndex: Seq[Int])

  sealed trait NodeRole[Command] {
    val stateCode: StateCode
    def appendEntries(rc: RaftContext[Command],
                      entriesToAppend: EntriesToAppend[Command]): (RaftContext[Command], AppendEntriesResult)
    def clientAppendEntries(rc: RaftContext[Command],
                            entries: Seq[Command]): Future[(RaftContext[Command], Either[Redirect[Command], Unit])]
  }

  private class Follower[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext)
      extends NodeRole[Command] {

    override def appendEntries(
        rc: RaftContext[Command],
        entriesToAppend: EntriesToAppend[Command]): (RaftContext[Command], AppendEntriesResult) = {
      applyAppendEntriesRules1To5(rc, entriesToAppend)
    }

    // AppendEntries RPC receiver implementation (figure 2), rules 1-5
    private def applyAppendEntriesRules1To5(
        rc: RaftContext[Command],
        entriesToAppend: EntriesToAppend[Command]): (RaftContext[Command], AppendEntriesResult) = {

      val log = rc.log
      val (currentTerm, _) = rc.persistentState

      if (appendEntriesConsistencyCheck1(entriesToAppend.term, currentTerm)) {

        (rc, AppendEntriesResult(term = currentTerm, success = false))

      } else if (appendEntriesConsistencyCheck2(log, entriesToAppend.prevLogIndex)) {

        (rc, AppendEntriesResult(term = currentTerm, success = false))

      } else {
        val conflicts = appendEntriesConflictSearch(log, entriesToAppend)
        if (conflicts != 0) {
          val rc2 = rc.copy(leaderId = entriesToAppend.leaderId, log = log.dropRight(conflicts))
          (rc2, AppendEntriesResult(term = currentTerm, success = false))
        } else {
          val additions: Seq[LogEntry[Command]] = appendEntriesAdditions(log, entriesToAppend)
          val rc2 = rc.copy(leaderId = entriesToAppend.leaderId, log = log ++ additions)
          val rc3 = appendEntriesCommitIndexCheck(rc2, entriesToAppend.leaderCommitIndex, iLastNewEntry(additions))
          (rc3, AppendEntriesResult(term = currentTerm, success = true))
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
    private def appendEntriesCommitIndexCheck(rc: RaftContext[Command],
                                              leaderCommitIndex: Int,
                                              iLastNewEntry: Int): RaftContext[Command] = {
      val commitIndex = Math.min(leaderCommitIndex, iLastNewEntry)

      if (leaderCommitIndex > rc.commonVolatileState.commitIndex) {
        rc.copy(commonVolatileState = rc.commonVolatileState.copy(commitIndex = commitIndex))
      } else {
        rc
      }
    }
    override def clientAppendEntries(
        rc: RaftContext[Command],
        entries: Seq[Command]): Future[(RaftContext[Command], Either[Redirect[Command], Unit])] =
      Future((rc, Left(Redirect(raftNode.getLeader))))
    override val stateCode: StateCode = Follower
  }

  private class Candidate[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext)
      extends NodeRole[Command] {
    override def appendEntries(
        rc: RaftContext[Command],
        entriesToAppend: EntriesToAppend[Command]): (RaftContext[Command], AppendEntriesResult) = {
      // rules for servers, candidates
      // if append entries rpc received from new leader, convert to follower
      val prospectiveLeaderTerm = entriesToAppend.term
      val (currentTerm, _) = rc.persistentState
      if (prospectiveLeaderTerm >= currentTerm) {
        // the term will have been updated via rules for servers note 2.
        val rc2 = raftNode.nodeFSM(rc, LeaderDiscovered)
        (rc2, AppendEntriesResult(currentTerm, success = true))
      } else {
        (rc, AppendEntriesResult(currentTerm, success = false))
      }
    }
    override def clientAppendEntries(
        rc: RaftContext[Command],
        entries: Seq[Command]): Future[(RaftContext[Command], Either[Redirect[Command], Unit])] =
      Future((rc, Left(Redirect(raftNode.getLeader))))

    override val stateCode: StateCode = Candidate
  }

  private class Leader[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext) extends NodeRole[Command] {

    override val stateCode: StateCode = Leader

    override def appendEntries(
        rc: RaftContext[Command],
        entriesToAppend: EntriesToAppend[Command]): (RaftContext[Command], AppendEntriesResult) = {
      val prospectiveLeaderTerm = entriesToAppend.term
      val (currentTerm, _) = rc.persistentState
      if (prospectiveLeaderTerm >= currentTerm) {
        // the term will have been updated via rules for servers note 2.
        val rc2 = raftNode.nodeFSM(rc, NodeWithHigherTermDiscovered)
        (rc2, AppendEntriesResult(currentTerm, success = true))
      } else {
        (rc, AppendEntriesResult(currentTerm, success = false))
      }
    }

    override def clientAppendEntries(
        rc: RaftContext[Command],
        entries: Seq[Command]): Future[(RaftContext[Command], Either[Redirect[Command], Unit])] = {
      val log = rc.log
      val (lastLogIndex, _) = raftNode.lastLogIndexAndTerm(log)
      val (currentTerm, _) = rc.persistentState

      val (_, entriesToAppend) = entries.foldLeft((lastLogIndex, Vector[LogEntry[Command]]()))((acc, nextCommand) => {
        val (logIndex, entriesToAppend) = acc
        val nextLogIndex = logIndex + 1
        val nextEntry = LogEntry[Command](nextCommand, currentTerm, nextLogIndex)
        (nextLogIndex, entriesToAppend :+ nextEntry)
      })

      val rc2 = rc.copy(log = log ++ entriesToAppend)

      sendAppendEntries(rc2).map(rcf => (rcf, Right(())))
    }

    private def sendAppendEntries(rc: RaftContext[Command]): Future[RaftContext[Command]] = {
      val nextIndexes = rc.leaderVolatileState.nextIndex
      val (currentTerm, _) = rc.persistentState
      val commitIndex = rc.commonVolatileState.commitIndex
      val theLog = rc.log
      val (lastLogIndex, _) = raftNode.lastLogIndexAndTerm(theLog)

      val callFs: Seq[Future[(Int, Int)]] = raftNode.clusterMembers.indices.map(i =>
        sendAppendEntry(rc, nextIndexes(i), lastLogIndex, raftNode.clusterMembers(i)))

      val indicesF: Future[(Seq[Int], Seq[Int])] =
        Future.sequence(callFs).map((indices: Seq[(Int, Int)]) => indices.unzip)

      for {
        (nextIndex, matchIndex) <- indicesF
      } yield {
        // Leaders, final note (#4)
        val maybeN = findN(theLog, currentTerm, commitIndex, matchIndex)

        val nextCommonState =
          maybeN.fold(rc.commonVolatileState)(n => rc.commonVolatileState.copy(commitIndex = n))

        val nextLeaderState = LeaderVolatileState(nextIndex, matchIndex)

        rc.copy(commonVolatileState = nextCommonState, leaderVolatileState = nextLeaderState)
      }
    }

    private def sendAppendEntry(rc: RaftContext[Command],
                                nextIndex: Int,
                                lastLogIndex: Int,
                                memberRPC: RPC[Command]): Future[(Int, Int)] = {
      val commitIndex = rc.commonVolatileState.commitIndex

      val (currentTerm, _) = rc.persistentState
      val appendResultF =
        appendEntryForNode(nextIndex, commitIndex, rc.log, currentTerm, lastLogIndex, memberRPC, getHeartbeat(rc))

      appendResultF.flatMap(appendResult => {
        if (appendResult.success) {
          // Leaders, If successful, update nextIndex and matchIndex for follower.
          Future((lastLogIndex + 1, lastLogIndex))
        } else {
          // Leaders, if fails because of log inconsistency, retry.
          sendAppendEntry(rc, nextIndex - 1, lastLogIndex, memberRPC)
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
    private def getHeartbeat(rc: RaftContext[Command]): EntriesToAppend[Command] = {
      val (currentTerm, _) = rc.persistentState
      val (prevLogIndex, prevLogTerm) = raftNode.lastLogIndexAndTerm(rc.log)
      val commitIndex = rc.commonVolatileState.commitIndex
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

  case object ElectionTimeout extends NodeEvent
  case object MajorityVoteReceived extends NodeEvent
  case object NodeWithHigherTermDiscovered extends NodeEvent
  case object LeaderDiscovered extends NodeEvent

  trait StateCode
  case object Follower extends StateCode
  case object Candidate extends StateCode
  case object Leader extends StateCode

  private type Transition[Command] = (RaftContext[Command], NodeEvent) => RaftContext[Command]

  class RaftFSM[Command](becomeFollower: Transition[Command],
                         becomeCandidate: Transition[Command],
                         becomeLeader: Transition[Command]) {

    val identity: Transition[Command] = (rc, _) => rc

    private val keyFn: RaftContext[Command] => StateCode = rc => rc.role.stateCode

    private val followerState: Transition[Command] = eventCata(electionTimeout = becomeCandidate)

    private val candidateState: Transition[Command] = eventCata(electionTimeout = becomeCandidate,
                                                                majorityVoteReceived = becomeLeader,
                                                                leaderDiscovered = becomeFollower)

    private val leaderState: Transition[Command] = eventCata(nodeWithHigherTermDiscovered = becomeFollower)

    private val table = Map[StateCode, Transition[Command]](
      Follower -> followerState,
      Candidate -> candidateState,
      Leader -> leaderState
    )

    private def eventCata(electionTimeout: Transition[Command] = identity,
                          majorityVoteReceived: Transition[Command] = identity,
                          nodeWithHigherTermDiscovered: Transition[Command] = identity,
                          leaderDiscovered: Transition[Command] = identity)(rc: RaftContext[Command],
                                                                                e: NodeEvent): RaftContext[Command] =
      e match {
        case ElectionTimeout =>
          electionTimeout(rc, e)
        case MajorityVoteReceived =>
          majorityVoteReceived(rc, e)
        case NodeWithHigherTermDiscovered =>
          nodeWithHigherTermDiscovered(rc, e)
        case LeaderDiscovered =>
          leaderDiscovered(rc, e)
      }

    def apply(rc: RaftContext[Command], e: NodeEvent): RaftContext[Command] =
      table(keyFn(rc)).apply(rc, e)
  }
}
