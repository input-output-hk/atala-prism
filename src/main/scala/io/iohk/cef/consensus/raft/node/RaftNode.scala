package io.iohk.cef.consensus.raft.node
import io.iohk.cef.consensus.raft.node.FutureOps.sequenceForgiving
import io.iohk.cef.consensus.raft._

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.stm.{Ref, Txn, atomic}
import org.slf4j.LoggerFactory

/**
  * Raft node implementation.
  */
private[raft] class RaftNode[Command](
    val nodeId: String,
    clusterMemberIds: Seq[String],
    rpcFactory: RPCFactory[Command],
    electionTimerFactory: RaftTimerFactory = defaultElectionTimerFactory,
    heartbeatTimerFactory: RaftTimerFactory = defaultHeartbeatTimerFactory,
    stateMachine: Command => Unit,
    persistentStorage: PersistentStorage[Command])(implicit ec: ExecutionContext)
    extends RaftNodeInterface[Command] {

  private val logger = LoggerFactory.getLogger(classOf[RaftNode[Command]])

  val clusterTable: Map[String, RPC[Command]] = clusterMemberIds
    .filterNot(_ == nodeId)
    .map(peerId => peerId -> rpcFactory(peerId, appendEntries, requestVote))
    .toMap

  val clusterMembers: Seq[RPC[Command]] = clusterTable.values.toSeq

  private val raftState: Ref[RaftState[Command]] = Ref(initialRaftState())

  private val sequencer: Ref[Future[_]] = Ref(Future(()))

  private val electionTimer = electionTimerFactory(() => electionTimeout())

  private val _ = heartbeatTimerFactory(() => heartbeatTimeout())

  val nodeFSM = new RaftFSM[Command](becomeFollower, becomeCandidate, becomeLeader)

  def getLeaderRPC: RPC[Command] = {
    val ctx = raftState.single()
    if (ctx.leaderId.nonEmpty)
      clusterTable(ctx.leaderId)
    else {
      val (_, votedFor) = ctx.persistentState
      clusterTable(votedFor)
    }
  }

  def getRole: StateCode =
    raftState.single().role.stateCode

  def getPersistentState: (Int, String) =
    raftState.single().persistentState

  def getCommonVolatileState: CommonVolatileState[Command] =
    raftState.single().commonVolatileState

  def getLeaderVolatileState: LeaderVolatileState =
    raftState.single().leaderVolatileState

  // Functions to atomically change the server state.
  // In the case of pure requests like appendEntries and requestVote,
  // requests are executed in strict sequence.
  // This enforces response invariants like only replying in the affirmative
  // to a requestVote from a single candidate.
  private[raft] def withState[T](f: RaftState[Command] => (RaftState[Command], T)): T = this.synchronized {
    atomic { implicit txn =>
      val initialContext = raftState()
      val (nextContext, result) = f(initialContext)
      raftState() = nextContext
      Txn.afterCommit(_ => persistState(nextContext))
      result
    }
  }

  // In the case of asynchronous entry points, where the answer returned depends
  // on responses from other nodes (clientAppendEntries and electionTimeout)
  // we use the properties of flatMap in conjunction with an atomic swap of
  // the future being mapped over to ensure response invariants.
  private[raft] def withFutureState[T](f: RaftState[Command] => Future[(RaftState[Command], T)]): Future[T] =
    this.synchronized {
      atomic { implicit txn =>
        // by the implementation of Future.flatMap, this will be executed after
        // any existing Future on the sequencer.
        val nextSeq = sequencer().flatMap(_ => f(raftState.single()).map(t => withState(_ => t)))
        // once sequenced, the operation is atomically set at the next Future on the sequencer.
        sequencer() = nextSeq
        nextSeq
      }
    }

  // node entry points
  // appendEntries       (called from rpc inbound) (not async)
  // requestVote         (called from rpc inbound) (not async)
  // heartbeatTimeout    (called from a timer)     (async)
  // clientAppendEntries (called from externally)  (async)
  // electionTimeout     (called from a timer)     (async)

  // Handler for client requests
  // This happens withFutureRaftContext because log entries cannot be committed until
  // From page 7 of the paper:
  // A log entry is committed once the leader that created the entry has replicated it on a majority of
  // the servers (e.g., entry 7 in Figure 6)
  // Thus, we can only commit state changes in the Future.
  def clientAppendEntries(entries: Seq[Command]): Future[Either[Redirect[Command], Unit]] = withFutureState { rs =>
    {
      rs.role.clientAppendEntries(rs, entries).map {
        case (ctx, response) =>
          (applyUncommittedLogEntries(ctx), response)
      }
    }
  }

  // Handler for inbound appendEntries RPCs from leaders.
  def appendEntries(entriesToAppend: EntriesToAppend[Command]): AppendEntriesResult = withState { rs =>
    val rs2 = rulesForServersAllServers2(rs, entriesToAppend.term)
    val (rs3, appendResult) = rs2.role.appendEntries(rs2, entriesToAppend)
    val rs4 = applyUncommittedLogEntries(rs3)
    (rs4, appendResult)
  }

  // Handler for inbound requestVote RPCs from candidates.
  def requestVote(voteRequested: VoteRequested): RequestVoteResult = withState { rs =>
    getVoteResult(rulesForServersAllServers2(rs, voteRequested.term), voteRequested)
  }

  private def electionTimeout(): Unit = {
    withState(rs => (nodeFSM.apply(rs, ElectionTimeout), ()))
    withFutureState(rs => requestVotes(rs))
  }

  private def heartbeatTimeout(): Unit = {
    withFutureState(rs => sendHeartbeat(rs))
  }

  private def sendHeartbeat(rs: RaftState[Command]): Future[(RaftState[Command], Unit)] = {
    rs.role.clientAppendEntries(rs, Seq()).map { case (ctx, _) => (ctx, ()) }
  }

  private def getVoteResult(
      rs: RaftState[Command],
      voteRequested: VoteRequested): (RaftState[Command], RequestVoteResult) = {

    val (currentTerm, votedFor) = rs.persistentState
    val (lastLogIndex, lastLogTerm) = lastLogIndexAndTerm(rs.log)

    if (voteRequested.term < currentTerm) { // receiver implementation, note #1
      (rs, RequestVoteResult(currentTerm, voteGranted = false))
    } else if ((votedFor.isEmpty || votedFor == voteRequested.candidateId)
      && ((lastLogTerm <= voteRequested.lastLogTerm) && (lastLogIndex <= voteRequested.lastLogIndex))) {
      (
        rs.copy(persistentState = (voteRequested.term, voteRequested.candidateId)),
        RequestVoteResult(voteRequested.term, voteGranted = true))
    } else {
      (rs, RequestVoteResult(currentTerm, voteGranted = false))
    }
  }

  private def requestVotes(voteRequested: VoteRequested): Future[Seq[RequestVoteResult]] = {
    val rpcFutures = clusterMembers.map(memberRpc => memberRpc.requestVote(voteRequested))
    sequenceForgiving(rpcFutures)
  }

  private def requestVotes(rs: RaftState[Command]): Future[(RaftState[Command], Unit)] = {
    val (term, _) = rs.persistentState
    val (lastLogIndex, lastLogTerm) = lastLogIndexAndTerm(rs.log)
    requestVotes(VoteRequested(term, nodeId, lastLogIndex, lastLogTerm)).flatMap(
      votes =>
        if (hasMajority(term, votes)) {
          val rs2 = nodeFSM(rs, MajorityVoteReceived)
          log(s"Changing state from ${rs.role.stateCode} to ${rs2.role.stateCode}")
          sendHeartbeat(rs2)
        } else
          Future((rs, ()))
    )
  }

  private def applyLogEntryToStateMachine(iEntry: Int, log: IndexedSeq[LogEntry[Command]]): Unit =
    stateMachine(log(iEntry).command)

  // Rules for servers, all servers, note 1.
  // If commitIndex > lastApplied, apply log[lastApplied] to the state machine.
  // (do this recursively until commitIndex == lastApplied)
  def applyUncommittedLogEntries(rs: RaftState[Command]): RaftState[Command] = {
    @tailrec
    def loop(state: CommonVolatileState[Command]): CommonVolatileState[Command] = {
      if (state.commitIndex > state.lastApplied) {
        val nextApplication = state.lastApplied + 1
        applyLogEntryToStateMachine(nextApplication, rs.log)
        loop(state.copy(lastApplied = nextApplication))
      } else {
        state
      }
    }
    val commonVolatileState = rs.commonVolatileState
    val nextState = loop(commonVolatileState)
    rs.copy(commonVolatileState = nextState)
  }

  // Rules for servers (figure 2), all servers, note 2.
  // If request (or response) contains term T > currentTerm
  // set currentTerm = T (convert to follower)
  private def rulesForServersAllServers2(rs: RaftState[Command], term: Int): RaftState[Command] = {
    val (currentTerm, votedFor) = rs.persistentState
    if (term > currentTerm) {
      nodeFSM(rs.copy(persistentState = (term, votedFor)), NodeWithHigherTermDiscovered)
    } else {
      rs
    }
  }

  // Note, this is different to the paper which specifies
  // one-based array ops, whereas we use zero-based.
  private def initialCommonState(): CommonVolatileState[Command] = {
    val initialCommitIndex = -1
    val initialLastApplied = -1
    CommonVolatileState(initialCommitIndex, initialLastApplied)
  }

  private def initialLeaderState(log: IndexedSeq[LogEntry[Command]]): LeaderVolatileState = {
    val (lastLogIndex, _) = lastLogIndexAndTerm(log)
    val nextIndex = Seq.fill(clusterMembers.size)(lastLogIndex + 1)
    val matchIndex = Seq.fill(clusterMembers.size)(-1)
    LeaderVolatileState(nextIndex, matchIndex)
  }

  private def initialRaftState(): RaftState[Command] = {
    val (currentTerm, votedFor) = persistentStorage.state
    val log = persistentStorage.log
    RaftState(
      new Follower(this),
      initialCommonState(),
      initialLeaderState(log),
      (currentTerm, votedFor),
      log,
      0,
      Seq(),
      votedFor
    )
  }

  private def becomeFollower(rs: RaftState[Command], event: NodeEvent): RaftState[Command] = {
    log(s"Changing state from ${rs.role.stateCode} to $Follower")
    rs.copy(role = new Follower(this))
  }

  // On conversion to candidate, start election:
  // Increment currentTerm
  // Vote for self
  // Reset election timer
  // Send RequestVote RPCs to all other servers
  private def becomeCandidate(rs: RaftState[Command], event: NodeEvent): RaftState[Command] = {
    log(s"Changing state from ${rs.role.stateCode} to $Candidate")
    electionTimer.reset()
    val (currentTerm, _) = rs.persistentState
    val newTerm = currentTerm + 1
    val nextPersistentState = (newTerm, nodeId)
    rs.copy(role = new Candidate(this), persistentState = nextPersistentState)
  }

  private def hasMajority(term: Int, votes: Seq[RequestVoteResult]): Boolean = {
    val myOwnVote = 1
    votes.count(vote => vote.voteGranted && vote.term == term) + myOwnVote > (clusterMembers.size + myOwnVote) / 2
  }

  def lastLogIndexAndTerm(log: IndexedSeq[LogEntry[Command]]): (Int, Int) = {
    log.lastOption.map(lastLogEntry => (lastLogEntry.index, lastLogEntry.term)).getOrElse((-1, -1))
  }

  private def becomeLeader(rs: RaftState[Command], event: NodeEvent): RaftState[Command] = {
    rs.copy(role = new Leader(this))
  }

  private def log(msg: String): Unit = {
    logger.info(s"Node $nodeId - $msg")
  }

  private def persistState(rs: RaftState[Command]): Unit = {
    val (term, votedFor) = rs.persistentState
    persistentStorage.state(term, votedFor)
    persistentStorage.log(rs.deletes, rs.writes)
  }
}

case class CommonVolatileState[Command](commitIndex: Int, lastApplied: Int)

case class LeaderVolatileState(nextIndex: Seq[Int], matchIndex: Seq[Int])
