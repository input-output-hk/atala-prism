package io.iohk.cef.raft.akka.fsm.protocol

import akka.actor.ActorRef

trait StateMetadata extends Serializable {
  type Member = ActorRef

  sealed trait MetaData {

    def votedFor: Option[Member]

    def currentTerm: Term

    /**
      * A member can only vote once during one Term
      *
      * @param term
      * @return
      */
    def canVoteIn(term: Term): Boolean = votedFor.isEmpty && term == currentTerm

    implicit def self: ActorRef

    def config: ClusterConfiguration

    /** Since I'm the Leader  everyone but myself */
    def membersExceptSelf: Set[Member] = config.members filterNot { _ == self }

    def members: Set[Member] = config.members

  }

  case class StateData(currentTerm: Term,
                       self:ActorRef,
                       config: ClusterConfiguration,
                       votedFor: Option[Member] = None,
                       votesReceived: Int = 0) extends MetaData

}
