package io.iohk.cef.raft.akka.fsm.protocol

import akka.actor.ActorRef
trait RaftProtocol extends Serializable {

  sealed trait RaftEventMessage

  case class RequestVote(
                          term: Term,
                          candidateId: ActorRef,
                          lastLogTerm: Term,
                          lastLogIndex: Int
                        ) extends RaftEventMessage

  case class BeginAsFollowerEvent(term: Term, ref: ActorRef) extends RaftEventMessage
  case object ElectionTimeoutEvent extends RaftEventMessage

  case class VoteCandidateEvent(term: Term)    extends RaftEventMessage
  case class DeclineCandidateEvent(term: Term) extends RaftEventMessage
}