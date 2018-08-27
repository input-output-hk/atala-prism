package io.iohk.cef.consensus.raft

import akka.actor.ActorRef
import io.iohk.cef.consensus.raft.model.{Entry, LogIndexMap, ReplicatedLog}
import io.iohk.cef.consensus.raft.protocol._

trait Leader {
  this: RaftActor =>
  val leaderEvents: StateFunction = {
    case Event(msg @ BeginAsLeader(term, _), sd: StateData) =>
      if  (raftConfig.publishTestEvents) context.system.eventStream.publish(msg) //This if for testing purpose
      log.info("Became leader for {}", sd.currentTerm)
      initializeLeaderState(sd.config.members)
      startHeartbeat(sd)
      stay()

    // client request

    case Event(ClientMessage(client, cmd:Command @unchecked), sd: StateData) =>
      log.info("Appending command: [{}] from {} to replicated log...", cmd, client)
      val entry = Entry(cmd, sd.currentTerm, replicatedLog.nextIndex, Some(client))
      log.debug("adding to log: {}", entry)
      replicatedLog += entry
      log.debug("log status = {}", replicatedLog)
      stay()

    case Event(SendHeartbeat, sd: StateData) =>
      sendHeartbeat(sd)
      stay()

    // Leader handling
    case Event(append: AppendEntries[Command @unchecked], sd: StateData) if append.term > sd.currentTerm =>
      if  (raftConfig.publishTestEvents) context.system.eventStream.publish(append) //This if for testing purpose
      log.info(
        "Leader (@ {}) got AppendEntries from fresher Leader " +
          "(@ {}), will step down and the Leader will keep being: {}",
        sd.currentTerm,
        append.term,
        sender()
      )
      stopHeartbeat()
      stepDown(sd)

    case Event(append: AppendEntries[Command @unchecked], sd: StateData) if append.term <= sd.currentTerm =>
      log.warning(
        "Leader (@ {}) got AppendEntries from rogue Leader ({} @ {}); It's not fresher than self." +
          " Will send entries, to force it to step down.",
        sd.currentTerm,
        sender(),
        append.term
      )
      sendEntries(sender(), sd)
      stay()
    // end of Leader handling

    // append entries response handling

    case Event(AppendRejected(term), sd: StateData) if term > sd.currentTerm =>
      stopHeartbeat()
      stepDown(sd) // since there seems to be another leader!

    case Event(msg: AppendRejected, sd: StateData) if msg.term == sd.currentTerm =>
      registerAppendRejected(follower(), msg, sd)

    case Event(msg: AppendSuccessful, sd: StateData) if msg.term == sd.currentTerm =>
      registerAppendSuccessful(follower(), msg, sd)
    // End append entries response handling

//
    case Event(RequestConfiguration, sd: StateData) =>
      sender() ! ChangeConfiguration(sd.config)
      stay()

  }


  private val HeartbeatTimerName = "heartbeat-timer"

  def initializeLeaderState(members: Set[ActorRef]) {
    log.info(
      "Preparing nextIndex and matchIndex table for followers, init all to: logEntries.lastIndex = {}",
      replicatedLog.lastIndex
    )
    nextIndex = LogIndexMap.initialize(members, replicatedLog.lastIndex)
    matchIndex = LogIndexMap.initialize(members, -1)

  }

  def stopHeartbeat() {
    cancelTimer(HeartbeatTimerName)
  }

  def startHeartbeat(sd: StateData) {
    sendHeartbeat(sd)
    log.info("Starting hearbeat, with interval: {}", heartbeatInterval)
    setTimer(HeartbeatTimerName, SendHeartbeat, heartbeatInterval, repeat = true)
  }

  /** Based RAFT  paper heartbeat is implemented as basically sending blank AppendEntry messages */
  def sendHeartbeat(sd: StateData) {
    replicateLog(sd)
  }

  def replicateLog(sd: StateData) {
    sd.membersExceptSelf foreach { member =>
      log.info(
        "Leader current term (@ {}) send  AppendEntries for  member (@ {})",
        sd.currentTerm,
        member.path)

      member ! AppendEntries(
        sd.currentTerm,
        replicatedLog,
        fromIndex = nextIndex.valueFor(member),
        leaderCommitIdx = replicatedLog.committedIndex,
        leaderId = self
      )
    }
  }

  /**
    * RAFT Paper
    * If AppendEntries fails because of log inconsistency:
    * decrement nextIndex and retry
    */
  def registerAppendRejected(member: ActorRef, msg: AppendRejected, sd: StateData): State = {
    val AppendRejected(followerTerm) = msg

    log.info(
      "Follower {} rejected write: {}, back out the first index in this term and retry",
      follower(),
      followerTerm
    )
    if (nextIndex.valueFor(follower()) > 1) {
      nextIndex.decrementFor(follower())
    }
    sendEntries(follower(), sd)
    stay()
  }

  def sendEntries(follower: ActorRef, sd: StateData) {
    follower ! AppendEntries(
      sd.currentTerm,
      replicatedLog,
      fromIndex = nextIndex.valueFor(follower),
      leaderCommitIdx = replicatedLog.committedIndex,
      leaderId = sd.self
    )
  }

  def registerAppendSuccessful(member: ActorRef, msg: AppendSuccessful, sd: StateData): State = {
    val AppendSuccessful(followerTerm, followerIndex) = msg

    log.info("Follower {} took write in term: {}", follower(), followerTerm)
    assert(followerIndex <= replicatedLog.lastIndex)
    // update our map for this member
    nextIndex.put(follower(), followerIndex + 1)
    matchIndex.putIfGreater(follower(), followerIndex)

    replicatedLog = commitEntry(sd, matchIndex, replicatedLog)

    stay()
  }

  def commitEntry(sd: StateData,
                  matchIndex: LogIndexMap,
                  replicatedLog: ReplicatedLog[Command]): ReplicatedLog[Command] = {
    val indexOnMajority = matchIndex.consensusForIndex(sd.config)
    val willCommit = indexOnMajority > replicatedLog.committedIndex

    if (willCommit) {
      log.info("Consensus for persisted index: {}. (Comitted index: {}, will commit now: {})",
               indexOnMajority,
               replicatedLog.committedIndex,
               willCommit)
      val entries = replicatedLog.between(replicatedLog.committedIndex, indexOnMajority)

      entries foreach { entry =>
        applyStateMachine(entry)

        if (raftConfig.publishTestEvents)
          context.system.eventStream.publish(EntryCommitted(entry.index, sd.self))
      }
      replicatedLog.commit(indexOnMajority)

    } else {
      replicatedLog
    }
  }



  private val applyStateMachine: PartialFunction[Any, Unit] = {
    case entry: Entry[Command @unchecked]=>
      log.info("Committing log at index: {}; Applying command: {}, will send result to client: {}", entry.index, entry.command, entry.client)
      val result = apply(entry.command)
      entry.client foreach { _ ! result }
  }

  def leaderStateHandler(): Unit = {
    self ! BeginAsLeader(stateData.currentTerm, self)
  }

}
