package io.iohk.cef.raft.akka.fsm

import akka.actor.ActorRef
import io.iohk.cef.raft.akka.fsm.model.Term

/**
  * Events for persisting changes to FSM internal state
  * replace the ActorRef here by a node id MileStone 4
  */
sealed trait DomainEvent
case class UpdateTermEvent(t: Term) extends DomainEvent
case class VoteForEvent(voteFor: ActorRef) extends DomainEvent
case class VoteForSelfEvent() extends DomainEvent
case class IncrementVoteEvent() extends DomainEvent
case class GoToFollowerEvent(t: Option[Term] = None) extends DomainEvent
case class GoToLeaderEvent() extends DomainEvent
case class StartElectionEvent() extends DomainEvent
case class KeepStateEvent() extends DomainEvent