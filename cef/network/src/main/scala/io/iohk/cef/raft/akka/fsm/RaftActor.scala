package io.iohk.cef.raft.akka.fsm

import akka.actor.{Actor, ActorRef}
import akka.persistence.fsm.PersistentFSM

import scala.concurrent.duration._
import protocol._

import scala.reflect._

abstract class RaftActor extends Actor with PersistentFSM[RaftState, StateData, DomainEvent]
  with Follower with Candidate with Leader with InitialState {



  type PersistentFSMState = PersistentFSM.State[RaftState, StateData, DomainEvent]


  private val ElectionTimeoutTimerName = "election-timer"
  val heartbeatInterval: FiniteDuration =  10.seconds  //TODO configurable  I am thinking of RaftConfig any thoughts

  var electionDeadline: Deadline = 0.seconds.fromNow
  var logEntries = LogEntries.empty[Command](10) //TODO configurable


  def nextElectionDeadline(): Deadline = ElectionTimeout.DefaultElectionTimeout.randomTimeout().fromNow

  def resetElectionDeadline(): Deadline = {
    cancelTimer(ElectionTimeoutTimerName)

    electionDeadline = nextElectionDeadline()
    log.debug("Resetting election timeout: {}", electionDeadline)

    setTimer(ElectionTimeoutTimerName, ElectionTimeout, electionDeadline.timeLeft, repeat = false)

    electionDeadline
  }

  override def domainEventClassTag: ClassTag[DomainEvent] = classTag[DomainEvent]

  override def persistenceId: String = "RaftActor-" + self.path.name

  override def applyEvent(domainEvent: DomainEvent, sd: StateData):StateData = domainEvent match  {
    case GoToFollowerEvent(term) => term.fold(sd.forFollower()){ t => sd.forFollower(t)}
    case GoToLeaderEvent() => sd.forLeader
    case StartElectionEvent() => sd.forNewElection
    case KeepStateEvent() => sd
    case VoteForEvent(candidate) => sd.withVoteFor(candidate)
    case IncrementVoteEvent() => sd.incVote
    case VoteForSelfEvent() => sd.incVote.withVoteFor(sd.self)
    case UpdateTermEvent(term) => sd.withTerm(term)
  }

  startWith(Init, StateData.initial(self))

  when(Init)(initialConfiguration)

  when(Follower)(followerEvents)

  when(Candidate)(candidateEvents)

  when(Leader)(leaderEvents)

  onTransition {
    case Init -> Follower if stateData.self != self =>
      self ! BeginAsFollower(stateData.currentTerm, self)
      resetElectionDeadline()

    case Follower -> Candidate =>
      self ! BeginElection
      resetElectionDeadline()

    case Candidate -> Leader =>
      self ! BeginAsLeader(stateData.currentTerm, self)
      cancelElectionDeadline()

    case _ -> Follower =>
      self ! BeginAsFollower(stateData.currentTerm, self)
      resetElectionDeadline()
  }

  onTermination {
    case stop =>
      stopHeartbeat()
  }


  def cancelElectionDeadline() {
    cancelTimer(ElectionTimeoutTimerName)
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



