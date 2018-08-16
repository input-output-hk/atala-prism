package io.iohk.cef.raft.akka.fsm

import akka.actor.ActorRef
import io.iohk.cef.raft.akka.fsm.model.{Entry, LogIndexMap}
import io.iohk.cef.raft.akka.fsm.protocol._

trait Leader {
  this: RaftActor =>
  private val HeartbeatTimerName = "heartbeat-timer"

  val leaderEvents: StateFunction = {
    case Event(BeginAsLeader(term, _), sd: StateData) =>
      log.info("Became leader for {}", sd.currentTerm)
      initializeLeaderState(sd.config.members)
      startHeartbeat(sd)
      stay()

    // client request

    case Event(ClientMessage(client, command), sd: StateData) =>
      log.info("Appending command: [{}] from {} to replicated log...", command, client)

      val entry = Entry(command, sd.currentTerm, replicatedLog.nextIndex, Some(client))

      log.debug("adding to log: {}", entry)
      replicatedLog += entry
      log.debug("log status = {}", replicatedLog)
      stay()

    case Event(SendHeartbeat, m: StateData) =>
      sendHeartbeat(m)
      stay()

    // Leader handling
    case Event(append: AppendEntries[_], sd: StateData) if append.term > sd.currentTerm =>
      log.info(
        "Leader (@ {}) got AppendEntries from fresher Leader " +
          "(@ {}), will step down and the Leader will keep being: {}",
        sd.currentTerm,
        append.term,
        sender()
      )
      stopHeartbeat()
      stepDown(sd)

    case Event(append: AppendEntries[_], sd: StateData) if append.term <= sd.currentTerm =>
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



    //TODO Somoe other event AS Leader based on RAFT paper

  }

  def initializeLeaderState(members: Set[ActorRef]) {
    log.info("Preparing nextIndex and matchIndex table for followers, init all to: logEntries.lastIndex = {}",
             replicatedLog.lastIndex)
      nextIndex = LogIndexMap.initialize(members, replicatedLog.lastIndex)

    //    Volatile state on leaders:
//    (Reinitialized after election)
//    nextIndex[]
//    matchIndex[]
//    for each server, index of the next log entry to send to that server (initialized to leader
//    last log index + 1)
//    for each server, index of highest log entry known to be replicated on server (initialized to 0, increases monotonically)
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


  def sendEntries(follower: ActorRef, sd: StateData) {
    follower ! AppendEntries(
      sd.currentTerm,
      replicatedLog,
      fromIndex = nextIndex.valueFor(follower),
      leaderCommitIdx = replicatedLog.committedIndex,
      leaderId = sd.self
    )
  }

  /**
    * RAFT Paper
    * If AppendEntries fails because of log inconsistency:
    * decrement nextIndex and retry
    */
  def registerAppendRejected(member: ActorRef, msg: AppendRejected, sd: StateData): State = {
    val AppendRejected(followerTerm) = msg

    log.info("Follower {} rejected write: {}, back out the first index in this term and retry",
             follower(),
             followerTerm)
    if (nextIndex.valueFor(follower()) > 1) {
      nextIndex.decrementFor(follower())
    }
    sendEntries(follower(), sd)
    stay()
  }

  def registerAppendSuccessful(member: ActorRef, msg: AppendSuccessful, sd: StateData): State = {
    val AppendSuccessful(followerTerm, followerIndex) = msg

    log.info("Follower {} took write in term: {}", follower(), followerTerm)
    assert(followerIndex <= replicatedLog.lastIndex)
    nextIndex.put(follower(), followerIndex + 1)

    //TODO
    //Commit Entries Here
    // update our tables for this member
    //If successful: update nextIndex and matchIndex for
    //follower
    stay()
  }

  def leaderStateHandler: Unit = {
    self ! BeginAsLeader(stateData.currentTerm, self)
  }

  def replicateLog(sd: StateData) {
    sd.membersExceptSelf foreach { member =>
      // todo remove me
      member ! AppendEntries(
        sd.currentTerm,
        replicatedLog,
        fromIndex = nextIndex.valueFor(member),
        leaderCommitIdx = replicatedLog.committedIndex,
        leaderId = self
      )
    }
  }


}
