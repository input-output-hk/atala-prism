package io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.RaftConsensus._

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.stm.{Ref, atomic}

/**
  * Raft node implementation.
  */
private[raft] class RaftNode[Command](val nodeId: String,
                                      val clusterMemberIds: Seq[String],
                                      rpcFactory: RPCFactory[Command],
                                      electionTimerFactory: RaftTimerFactory = defaultElectionTimerFactory,
                                      heartbeatTimerFactory: RaftTimerFactory = defaultHeartbeatTimerFactory,
                                      val stateMachine: Command => Unit,
                                      persistentStorage: PersistentStorage[Command])(implicit ec: ExecutionContext)
    extends RaftNodeInterface[Command] {

  val clusterTable: Map[String, RPC[Command]] = clusterMemberIds
    .filterNot(_ == nodeId)
    .map(peerId => peerId -> rpcFactory(peerId, appendEntries, requestVote))
    .toMap

  val clusterMembers: Seq[RPC[Command]] = clusterTable.values.toSeq

  private val rc: Ref[RaftContext[Command]] = Ref(initialRaftContext())

  private val electionTimer = electionTimerFactory(() => electionTimeout())

  private val _ = heartbeatTimerFactory(() => heartbeatTimeout())

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

  // Handler for client requests
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
        } else
          Future((rc, ()))
    )
  }

  private def applyLogEntryToStateMachine(iEntry: Int, log: Vector[LogEntry[Command]]): Unit =
    stateMachine(log(iEntry).command)

  // Rules for servers, all servers, note 1.
  // If commitIndex > lastApplied, apply log[lastApplied] to the state machine.
  // (do this recursively until commitIndex == lastApplied)
  def applyUncommittedLogEntries(rc: RaftContext[Command]): RaftContext[Command] = {

    @tailrec
    def loop(state: CommonVolatileState[Command]): CommonVolatileState[Command] = {
      if (state.commitIndex > state.lastApplied) {
        val nextApplication = state.lastApplied + 1
        applyLogEntryToStateMachine(nextApplication, rc.log)
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
