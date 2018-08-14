package io.iohk.cef.raft.akka.fsm

import akka.actor.{Actor, ActorRef}
import akka.persistence.fsm.PersistentFSM

import scala.concurrent.duration._
import protocol._

abstract class RaftActor extends Actor with PersistentFSM[RaftState, MetaData, DomainEvent]
  with Follower with Candidate with Leader {

  type PersistentFSMState = PersistentFSM.State[RaftState, MetaData, DomainEvent]

  type Command

  private val ElectionTimeoutTimerName = "election-timer"

  var electionDeadline: Deadline = 0.seconds.fromNow
  var logEntries = LogEntries.empty[Command](10) //TODO configurable


  def nextElectionDeadline(): Deadline = ElectionTimeout.DefaultElectionTimeout.randomTimeout().fromNow

  def resetElectionDeadline(): Deadline = {
    cancelTimer(ElectionTimeoutTimerName)

    electionDeadline = nextElectionDeadline()
    log.debug("Resetting election timeout: {}", electionDeadline)

    setTimer(ElectionTimeoutTimerName, ElectionTimeoutEvent, electionDeadline.timeLeft, repeat = false)

    electionDeadline
  }


  /** Start a new election */
  def beginElection(m: StateData):PersistentFSMState= {
    resetElectionDeadline()
      goto(Candidate) applying StartElectionEvent() forMax nextElectionDeadline().timeLeft
  }

  /** Stop being the Leader */
  def stepDown(sd: StateData):PersistentFSMState = {
    goto(Follower) applying GoToFollowerEvent()
  }

  /** Stay in current state and reset the election timeout */
  def acceptHeartbeat():PersistentFSMState = {
    resetElectionDeadline()
    stay()
  }

  // sender aliases, for readability
  @inline def follower():ActorRef = sender()
  @inline def candidate():ActorRef = sender()
  @inline def leader():ActorRef = sender()
  @inline def voter():ActorRef = sender()
  // end of sender aliases

}



