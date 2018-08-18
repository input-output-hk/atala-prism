package io.iohk.cef.raft.akka.fsm

import akka.actor.{Actor, ActorRef}
import akka.persistence.fsm.PersistentFSM

import scala.concurrent.duration._
import protocol._
import io.iohk.cef.raft.akka.fsm.model._

import scala.reflect._
import config.RaftConfiguration


abstract class RaftActor extends Actor with PersistentFSM[RaftState, StateData, DomainEvent]
  with Follower with Candidate with Leader with InitialState {

  type Command

  type PersistentFSMState = PersistentFSM.State[RaftState, StateData, DomainEvent]

  protected val raftConfig = RaftConfiguration(context.system)

  private val ElectionTimeoutTimerName = "election-timer"
  val heartbeatInterval: FiniteDuration =  raftConfig.heartbeatInterval

  var replicatedLog = ReplicatedLog.empty[Command](raftConfig.defaultAppendEntriesBatchSize)
  var nextIndex = LogIndexMap.initialize(Set.empty, replicatedLog.nextIndex)
  var matchIndex = LogIndexMap.initialize(Set.empty, 0)


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

  when(Follower ,stateTimeout = nextElectionTime)(followerEvents)

  when(Candidate ,stateTimeout = nextElectionTime)(candidateEvents)

  when(Leader)(leaderEvents)

  onTransition {
    case Init -> Follower => followerStateHandler

    case Follower -> Candidate => candidateStateHandler

    case Candidate -> Leader => leaderStateHandler

    case _ -> Follower => followerStateHandler

  }

  onTermination {
    case stop =>
      stopHeartbeat()
  }


// Helper for timeout
  def nextElectionTime(): FiniteDuration = randomElectionTimeout(
    from = raftConfig.electionTimeoutMin,
    to = raftConfig.electionTimeoutMax
  )

  private def randomElectionTimeout(from: FiniteDuration, to: FiniteDuration): FiniteDuration = {
    val fromMs = from.toMillis
    val toMs = to.toMillis
    require(toMs > fromMs, s"to ($to) must be greater than from ($from) in order to create valid election timeout.")

    (fromMs + java.util.concurrent.ThreadLocalRandom.current().nextInt(toMs.toInt - fromMs.toInt)).millis
  }
  // End Helper for timeout

  /** Start a new election */
  def beginElection(sd: StateData):PersistentFSMState= {
      goto(Candidate) applying StartElectionEvent()
  }

  /** Stop being the Leader */
  def stepDown(sd: StateData):PersistentFSMState = {
    goto(Follower) applying GoToFollowerEvent()
  }

  /** Stay in current state and reset the election timeout */
  def acceptHeartbeat():PersistentFSMState = {
    stay()
  }

  // sender aliases, for readability
  @inline def follower():ActorRef = sender()
  @inline def candidate():ActorRef = sender()
  @inline def leader():ActorRef = sender()
  @inline def voter():ActorRef = sender()
  // end of sender aliases

}



