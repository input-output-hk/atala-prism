package io.iohk.cef.consensus.raft
import java.util.concurrent.atomic.AtomicReference

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.javadsl.Behaviors.{receiveMessage, same, unhandled}

import scala.concurrent.Future

object RaftConsensus {

  case class LogEntry[Command](command: Command, term: Long, index: Int)

  trait PersistentStorage[Command] {
    def state: Future[(Long, String)]
    def state(currentTerm: Long, votedFor: String): Future[Unit]

    def log(command: Command): Future[Unit]
    def log: Future[Seq[Command]]
  }

  /**
    * From Figure 1.
    * TODO new entries must go via the leader, so the module will need
    * to talk to the correct node.
    */
  class ConsensusModule[Command](nodeId: String,
                                 clusterMembers: Set[String],
                                 stateMachine: Command => Unit,
                                 persistentStorage: PersistentStorage[Command]) {
    // Sec 5.1
    // The leader handles all client requests (if
    // a client contacts a follower, the follower redirects it to the leader)
    def appendEntries(entries: Seq[Command]): Future[Unit] = ???
  }

  class RaftNode[Command](nodeId: String,
                          clusterMemberIds: Set[String],
                          rpcFactory: (String, AppendEntries[Command], RequestVote) => RPCImpl[Command],
                          stateMachine: Command => Unit,
                          persistentStorage: PersistentStorage[Command])(implicit actorSystem: ActorSystem) {

    private val nodeFSM: ActorRef[NodeEvent] =
      actorSystem.spawnAnonymous(new NodeFSM[Command](becomeFollower, becomeCandidate, becomeLeader).stateMachine)

    private val stateFuture: Future[(Long, String)] = persistentStorage.state

    private val logFuture: Future[Seq[Command]] = persistentStorage.log

    private val clusterMembers: Set[RPCImpl[Command]] = clusterMemberIds
      .filterNot(_ == nodeId)
      .map(rpcFactory(_, appendEntries, requestVote))

    private val roleStrategies = Map()

    def appendEntries: AppendEntries[Command] = (entryToAppend: EntryToAppend[Command]) => {
      ???
    }

    def requestVote: RequestVote = (voteRequested: VoteRequested) => {
      ???
    }

    /*
     * Invoked by leader to replicate log entries (§5.3); also used as
     * heartbeat (§5.2).
     *
     * Receiver implementation:
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. Reply false if log doesn’t contain an entry at prevLogIndex
     * whose term matches prevLogTerm (§5.3)
     * 3. If an existing entry conflicts with a new one (same index
     * but different terms), delete the existing entry and all that
     * follow it (§5.3)
     * 4. Append any new entries not already in the log
     * 5. If leaderCommit > commitIndex, set commitIndex =
     * min(leaderCommit, index of last new entry)
     */
//    private def appendEntries

    /*
     * Invoked by candidates to gather votes (§5.2).
     *
     * Receiver implementation:
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. If votedFor is null or candidateId, and candidate’s log is at
     * least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
     */
    def becomeFollower(event: NodeEvent): Unit = {
    }

    def becomeCandidate(event: NodeEvent): Unit = {}

    def becomeLeader(event: NodeEvent): Unit = {}
  }

  abstract class NodeRole[Command](currentTerm: Long,
                                   votedFor: String,
                                   log: Vector[LogEntry[Command]],
                                   commitIndex: Long,
                                   lastApplied: Long) {}

  class FollowerRole[Command](currentTerm: Long,
                              votedFor: String,
                              log: Vector[LogEntry[Command]],
                              commitIndex: Long,
                              lastApplied: Long)
      extends NodeRole[Command](currentTerm, votedFor, log, commitIndex, lastApplied)

  class CandidateRole[Command](currentTerm: Long,
                               votedFor: String,
                               log: Vector[LogEntry[Command]],
                               commitIndex: Long,
                               lastApplied: Long)
      extends NodeRole[Command](currentTerm, votedFor, log, commitIndex, lastApplied)

  class LeaderRole[Command](currentTerm: Long,
                            votedFor: String,
                            log: Vector[LogEntry[Command]],
                            commitIndex: Long,
                            lastApplied: Long,
                            nextIndex: Seq[Long],
                            matchIndex: Seq[Long])
      extends NodeRole[Command](currentTerm, votedFor, log, commitIndex, lastApplied)


  case class EntryToAppend[Command](term: Long,
                                    leaderId: String,
                                    prevLogIndex: Long,
                                    prevLogTerm: Long,
                                    entries: Seq[LogEntry[Command]],
                                    leaderCommit: Long)
  case class AppendEntriesResult(term: Long, success: Boolean)
  type AppendEntries[Command] = EntryToAppend[Command] => Future[AppendEntriesResult]

  case class VoteRequested(term: Long, candidateId: String, lastLogIndex: Long, lastLogTerm: Long)
  case class RequestVoteResult(term: Long, voteGranted: Boolean)
  type RequestVote = VoteRequested => Future[RequestVoteResult]

  /**
    * To be implemented by users of the module using whatever networking method is deemed relevant.
    *
    * @param node identifier of the node at the other end of the RPC
    * @param entriesAppended callback to invoke when another node sends an appendEntries RPC
    * @param voteRequested callback to invoke when another node sends a voteRequested RPC
    * @tparam Command the user command type.
    */
  abstract class RPCImpl[Command](node: String, entriesAppended: AppendEntries[Command], voteRequested: RequestVote) {

    val appendEntries: AppendEntries[Command]

    val requestVote: RequestVote
  }

  sealed trait NodeEvent

  case object ElectionTimeout extends NodeEvent
  case object MajorityVoteReceived extends NodeEvent
  case class NodeWithHigherTermDiscovered(node: String) extends NodeEvent
  case class LeaderDiscovered(node: String) extends NodeEvent

  sealed trait NodeState

  case object Leader extends NodeState
  case object Candidate extends NodeState
  case object Follower extends NodeState

  class NodeFSM[Command](becomeFollower: NodeEvent => Unit,
                         becomeCandidate: NodeEvent => Unit,
                         becomeLeader: NodeEvent => Unit) {

    private def leaderState: Behavior[NodeEvent] = receiveMessage {
      case NodeWithHigherTermDiscovered =>
        becomeFollower()
        followerState
      case _ =>
        unhandled
    }

    private def candidateState: Behavior[NodeEvent] = receiveMessage {
      case ElectionTimeout =>
        becomeCandidate()
        same
      case MajorityVoteReceived =>
        becomeLeader()
        leaderState
      case _ =>
        unhandled
    }

    private def followerState: Behavior[NodeEvent] = receiveMessage {
      case ElectionTimeout =>
        becomeCandidate()
        candidateState
      case _ =>
        unhandled
    }

    def stateMachine: Behavior[NodeEvent] = followerState
  }
}
