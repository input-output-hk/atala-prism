package io.iohk.cef.consensus.raft.protocol

import akka.actor.ActorRef
import io.iohk.cef.consensus.raft.model.{Entry, ReplicatedLog, Term}

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
  /**
    * Wrap messages you want to send to the underlying replicated state machine
    */
  case class ClientMessage[T](client: ActorRef, cmd: T) extends Message



  case class AppendEntries[T](
                               term: Term,
                               prevLogTerm: Term,
                               prevLogIndex: Int,
                               entries: immutable.Seq[Entry[T]],
                               leaderCommitId: Int,
                               leaderId : ActorRef //Can be NodeId/ actor path in akka eco system
                             ) extends Message {
    override def toString: String =
      s"""AppendEntries(term:$term,prevLog:($prevLogTerm,$prevLogIndex),entries:$entries,leaderCommit:$leaderCommitId)"""
  }

  object AppendEntries {
    // Throws IllegalArgumentException if fromIndex > replicatedLog.length
    def apply[T](term: Term, replicatedLog: ReplicatedLog[T], fromIndex: Int,leaderCommitIdx: Int , leaderId:ActorRef): AppendEntries[T] = {
      if (fromIndex > replicatedLog.nextIndex) {
        throw new IllegalArgumentException(s"fromIndex ($fromIndex) > nextIndex (${replicatedLog.nextIndex})")
      }
      val entries = replicatedLog.entriesBatchFrom(fromIndex)
      val prevIndex = List(0, fromIndex - 1).max
      val prevTerm = replicatedLog.termAt(prevIndex)

      new AppendEntries[T](term, prevTerm, fromIndex, entries,
        leaderCommitIdx ,leaderId)
    }
  }

  sealed trait FollowerResponse extends Message

  /** When the Leader has sent an append, for an unexpected number, the Follower replies with this */
  sealed trait AppendResponse extends FollowerResponse {
    /** currentTerm for leader to update in the `nextTerm` lookup table */
    def term: Term
  }
  case class AppendRejected(term: Term)                   extends AppendResponse
  case class AppendSuccessful(term: Term, lastIndex: Int) extends AppendResponse

  case class ChangeConfiguration(newConf: ClusterConfiguration) extends Message
  case object RequestConfiguration extends  Message

  /**
    * Removes one member to the cluster; Used in discovery phase, during Init state of RaftActor in clustered setup.
    */
  case class RaftMemberAdded(member: ActorRef, keepInitUntil: Int) extends Message

  /**
    * Removes one member to the cluster; Used in discovery phase, during Init state of RaftActor in clustered setup.
    */
  case class RaftMemberRemoved(member: ActorRef, keepInitUntil: Int) extends Message



  // ----   Only For testing AKKA messages     ----
  case class TermUpdated(term: Term, ref: ActorRef) extends Message
  case class ElectionStarted(term: Term, ref: ActorRef) extends Message
  // ---- end of testing AKKA messages ----

}