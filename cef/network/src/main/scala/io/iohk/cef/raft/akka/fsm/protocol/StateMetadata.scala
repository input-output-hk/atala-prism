package io.iohk.cef.raft.akka.fsm.protocol

import akka.actor.ActorRef

trait StateMetadata extends Serializable {

  type CandidateRef = ActorRef
  sealed trait MetaData {
    def votedFor: Option[CandidateRef]

    def currentTerm: Term

    /**
      * A member can only vote once during one Term
      *
      * @param term
      * @return
      */
    def canVoteIn(term: Term): Boolean = votedFor.isEmpty && term == currentTerm

  }

  case class StateData(currentTerm: Term,
                       votedFor: Option[CandidateRef] = None,
                       votesReceived: Int = 0) extends MetaData

}
