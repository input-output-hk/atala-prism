package io.iohk.cef.raft.akka.fsm.protocol

import akka.actor.ActorRef

import scala.collection.immutable
trait RaftProtocol extends Serializable {

  sealed trait Message

  case class RequestVote(
                          term: Term,
                          candidateId: ActorRef,
                          lastLogTerm: Term,
                          lastLogIndex: Int
                        ) extends Message

  case class BeginAsFollower(term: Term, ref: ActorRef) extends Message
  case object ElectionTimeout extends Message

  case class VoteCandidate(term: Term)    extends Message
  case class DeclineCandidate(term: Term) extends Message

  case object BeginElection extends Message
  case class BeginAsLeader(term: Term, ref: ActorRef)   extends Message

  case object SendHeartbeat extends Message


  case class AppendEntries[T](
                               term: Term,
                               prevLogTerm: Term,
                               prevLogIndex: Int,
                               entries: immutable.Seq[Entry[T]],
                               leaderCommitId: Int
                             ) extends Message {
    override def toString: String =
      s"""AppendEntries(term:$term,prevLog:($prevLogTerm,$prevLogIndex),entries:$entries,leaderCommit:$leaderCommitId)"""
  }

  sealed trait FollowerResponse extends Message

  /** When the Leader has sent an append, for an unexpected number, the Follower replies with this */
  sealed trait AppendResponse extends FollowerResponse {
    /** currentTerm for leader to update in the `nextTerm` lookup table */
    def term: Term
  }
  case class AppendRejected(term: Term)                   extends AppendResponse
  case class AppendSuccessful(term: Term, lastIndex: Int) extends AppendResponse

}