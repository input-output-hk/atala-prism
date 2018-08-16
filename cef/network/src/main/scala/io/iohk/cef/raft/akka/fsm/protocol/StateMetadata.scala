package io.iohk.cef.raft.akka.fsm.protocol

import akka.actor.ActorRef
import io.iohk.cef.raft.akka.fsm.model.Term

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
                       votesReceived: Int = 0) extends MetaData{

    // transition helpers
    def forNewElection: StateData = StateData(currentTerm.next,self, config)

    def withTerm(term: Term): StateData = copy(currentTerm = term, votedFor = None)
    def incTerm: StateData = copy(currentTerm = currentTerm.next)

    def withVoteFor(candidate: ActorRef): StateData = copy(votedFor = Some(candidate))

    def hasMajority: Boolean = votesReceived > config.members.size / 2

    def incVote: StateData = copy(votesReceived = votesReceived + 1)

    def forLeader: StateData = StateData(currentTerm, self,config)
    def forFollower(term: Term = currentTerm): StateData = StateData(term,self, config)
  }

  object StateData {
    def initial(implicit self: ActorRef): StateData =
      new StateData(Term(0), self,ClusterConfiguration, None, 0)
  }
}
