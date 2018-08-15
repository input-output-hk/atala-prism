package io.iohk.cef.raft.akka.fsm.protocol

import akka.actor.ActorRef

import scala.collection.immutable
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

  case object BeginElectionEvent extends RaftEventMessage
  case class BeginAsLeader(term: Term, ref: ActorRef)   extends RaftEventMessage

  case object SendHeartbeat extends RaftEventMessage


  case class AppendEntries[T](
                               term: Term,
                               prevLogTerm: Term,
                               prevLogIndex: Int,
                               entries: immutable.Seq[Entry[T]],
                               leaderCommitId: Int
                             ) extends RaftEventMessage {
    override def toString: String =
      s"""AppendEntries(term:$term,prevLog:($prevLogTerm,$prevLogIndex),entries:$entries,leaderCommit:$leaderCommitId)"""
  }

  sealed trait FollowerResponse extends RaftEventMessage

  /** When the Leader has sent an append, for an unexpected number, the Follower replies with this */
  sealed trait AppendResponse extends FollowerResponse {
    /** currentTerm for leader to update in the `nextTerm` lookup table */
    def term: Term
  }
  case class AppendRejected(term: Term)                   extends AppendResponse
  case class AppendSuccessful(term: Term, lastIndex: Int) extends AppendResponse

}