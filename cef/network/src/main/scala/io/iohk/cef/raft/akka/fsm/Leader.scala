package io.iohk.cef.raft.akka.fsm

import akka.actor.ActorRef
import protocol._

trait Leader {
  this: RaftActor =>
  private val HeartbeatTimerName = "heartbeat-timer"

  val leaderEvents: StateFunction = {
    case Event(BeginAsLeader(term, _), sd: StateData) =>
      log.info("Became leader for {}", sd.currentTerm)
      initializeLeaderState(sd.config.members)
      startHeartbeat(sd)
      stay()

    case Event(SendHeartbeat, m: StateData) =>
      sendHeartbeat(m)
      stay()

    // Leader handling
    case Event(append: AppendEntries[Command], sd: StateData) if append.term > sd.currentTerm =>
      log.info("Leader (@ {}) got AppendEntries from fresher Leader " +
        "(@ {}), will step down and the Leader will keep being: {}", sd.currentTerm, append.term, sender())
      stopHeartbeat()
      stepDown(sd)

    case Event(append: AppendEntries[Command], sd: StateData) if append.term <= sd.currentTerm =>
      log.warning("Leader (@ {}) got AppendEntries from rogue Leader ({} @ {}); It's not fresher than self." +
        " Will send entries, to force it to step down.", sd.currentTerm, sender(), append.term)
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

    //TODO Somoe other event AS Leader based on RAFT papper

  }

  def initializeLeaderState(members: Set[ActorRef]) {
    log.info("Preparing nextIndex and matchIndex table for followers, init all to: logEntries.lastIndex = {}", logEntries.lastIndex)

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
    //logEntries(sd)
  }

  def sendEntries(sender: ActorRef, m: StateData) {
    //TODO Append Entries
  }

  def registerAppendRejected(member: ActorRef, msg: AppendRejected, sd: StateData): State = {
    val AppendRejected(followerTerm) = msg

    log.info("Follower {} rejected write: {}, back out the first index in this term and retry", follower(), followerTerm)

    /**
      * RAFT Paper
      * If AppendEntries fails because of log inconsistency:
      * decrement nextIndex and retry
      */
    // TODO
    sendEntries(follower(), sd)
    stay()
  }

  def registerAppendSuccessful(member: ActorRef, msg: AppendSuccessful, sd: StateData): State = {
    val AppendSuccessful(followerTerm, followerIndex) = msg

    log.info("Follower {} took write in term: {}", follower(), followerTerm)
    //TODO
    // update our tables for this member
    //If successful: update nextIndex and matchIndex for
    //follower
    stay()
  }
}
