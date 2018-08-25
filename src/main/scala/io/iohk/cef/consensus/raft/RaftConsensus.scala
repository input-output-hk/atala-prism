package io.iohk.cef.consensus.raft

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, same, setup, unhandled}
import io.iohk.cef.utils.StateActor

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

object RaftConsensus {

  case class LogEntry[Command](command: Command, term: Long, index: Long)

  trait PersistentStorage[Command] {
    def state: Future[(Long, String)]
    def state(currentTerm: Long, votedFor: String): Future[Unit]

    def log(entry: LogEntry[Command]): Future[Unit]
    def log: Future[Vector[LogEntry[Command]]]

    def dropRight(n: Int): Future[Unit]
  }

  case class Redirect(leader: String)

  /**
    * From Figure 1.
    * TODO new entries must go via the leader, so the module will need
    * to talk to the correct node.
    */
  class ConsensusModule[Command](raftNode: RaftNode[Command]) {
    // Sec 5.1
    // The leader handles all client requests (if
    // a client contacts a follower, the follower redirects it to the leader)
    def appendEntries(entries: Seq[Command]): Future[Either[Redirect, Unit]] = ???
  }

  class RaftNode[Command](val nodeId: String,
                          val clusterMemberIds: Set[String],
                          rpcFactory: RPCFactory[Command],
                          stateMachine: Command => Unit,
                          val persistentStorage: PersistentStorage[Command])(implicit actorSystem: ActorSystem) {

    private implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    private val clusterMembers: Set[RPC[Command]] = clusterMemberIds
      .filterNot(_ == nodeId)
      .map(rpcFactory(_, appendEntries, requestVote))

    val roleState =
      new StateActor[NodeState[Command]]()

    val commonVolatileState = new StateActor[CommonVolatileState[Command]]()

    val leaderVolatileState = new StateActor[LeaderVolatileState]()

    private val nodeFSM: ActorRef[NodeEvent] =
      actorSystem.spawnAnonymous(new NodeFSM[Command](becomeFollower, becomeCandidate, becomeLeader).stateMachine)

    def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] =
      roleState.get.flatMap(state => state.appendEntries(entriesToAppend))

    def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] =
      roleState.get.flatMap(state => state.requestVote(voteRequested))

    private def electionTimeout: Unit =
      nodeFSM ! ElectionTimeout

    def resetElectionTimer: Unit = ???

    def requestVotes: Unit = ???

    private def initialState(): Future[NodeState[Command]] = {
      val initialCommitIndex = 0
      val initialLastApplied = 0

      commonVolatileState.set(CommonVolatileState(initialCommitIndex, initialLastApplied)).map(_ => new Follower[Command](raftNode = this))
    }

    private def becomeFollower(event: NodeEvent): Unit = event match {
      case Start =>
        initialState().map(roleState.set)
      case _ =>
        roleState.set(new Follower(this))
    }

    // On conversion to candidate, start election:
    // Increment currentTerm
    // Vote for self
    // Reset election timer
    // Send RequestVote RPCs to all other servers
    private def becomeCandidate(event: NodeEvent): Unit = {
      for {
        (currentTerm, _) <- persistentStorage.state
      } yield persistentStorage.state(currentTerm + 1, nodeId)

      resetElectionTimer
      requestVotes
    }

    private def becomeLeader(event: NodeEvent): Unit = ???

  }

  class ElectionTimer(onTimeout: () => Unit) {
    def reset(): Unit = ???
  }

  case class CommonVolatileState[Command](commitIndex: Long, lastApplied: Long)

  case class LeaderVolatileState(nextIndex: Seq[Long], matchIndex: Seq[Long])

  sealed trait NodeState[Command] {
    def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult]
    def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult]
  }

  class Follower[Command](raftNode: RaftNode[Command])(
      implicit ec: ExecutionContext)
      extends NodeState[Command] {

    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] = {
      for {
        volatileState <- raftNode.commonVolatileState.get
        (currentTerm, votedFor) <- raftNode.persistentStorage.state
        log <- raftNode.persistentStorage.log
      } yield {
        if (appendEntriesConsistencyCheck1(entriesToAppend.term, currentTerm)) {
          AppendEntriesResult(term = currentTerm, success = false)
        } else if (appendEntriesConsistencyCheck2(log, entriesToAppend.prevLogIndex)) {
          AppendEntriesResult(term = currentTerm, success = false)
        } else {
          val conflicts = appendEntriesConflictSearch(log, entriesToAppend)
          if (conflicts != 0) {
            raftNode.persistentStorage.dropRight(conflicts)
            AppendEntriesResult(term = currentTerm, success = false)
          } else {
            ???
          }

        }
      }
    }

    // AppendEntries summary note #1 (Sec 5.1 consistency check)
    private def appendEntriesConsistencyCheck1(term: Long, currentTerm: Long): Boolean =
      term < currentTerm

    // AppendEntries summary note #2 (Sec 5.3 Consistency check)
    private def appendEntriesConsistencyCheck2(log: Vector[LogEntry[Command]], prevLogIndex: Long): Boolean = {
      @tailrec
      def loop(i: Long): Boolean = {
        if (i == -1)
          true
        else if (log(i.toInt).index == prevLogIndex)
          false
        else
          loop(i - 1)
      }
      loop(log.size - 1)
    }

    // AppendEntries summary note #3 (Sec 5.3 deleting inconsistent log entries)
    private def appendEntriesConflictSearch(log: Vector[LogEntry[Command]], entriesToAppend: EntriesToAppend[Command]): Int = {

      val logSz = log.size

      val minEntryIndex: Long = entriesToAppend.entries.headOption.map(head => head.index).getOrElse(logSz)

      @tailrec
      def loop(i: Long, iMin: Long): Long = { // reverse search for the minimum index of a conflicting entry

        if (i == -1)
          iMin
        else {
          val current: LogEntry[Command] = log(i.toInt)

          if (minEntryIndex > current.index) { // all entries to append have a higher index than current, terminate reverse search
            iMin
          } else {
            // same index but different terms check
            val maybeConflictingEntry: Option[LogEntry[Command]] =
              entriesToAppend.entries.find(entryToAppend => current.index == entryToAppend.index && current.term != entryToAppend.term)

            if (maybeConflictingEntry.isDefined) {
              loop(i - 1, i)
            } else {
              loop(i - 1, iMin)
            }
          }
        }
      }

      val iMin = loop(logSz - 1, logSz)

      (logSz - iMin).toInt
    }

    override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] = ???
  }

  class Candidate[Command](raftNode: RaftNode[Command])(implicit ec: ExecutionContext) extends NodeState[Command] {
    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] = ???
    override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] = ???
  }

  class Leader[Command](raftNode: RaftNode[Command],
                        val commonState: CommonVolatileState[Command],
                        val leaderState: LeaderVolatileState)(implicit ec: ExecutionContext)
      extends NodeState[Command] {
    override def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] = ???
    override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] = ???
  }

  case class EntriesToAppend[Command](term: Long,
                                      leaderId: String,
                                      prevLogIndex: Long,
                                      prevLogTerm: Long,
                                      entries: Seq[LogEntry[Command]],
                                      leaderCommitIndex: Long)
  case class AppendEntriesResult(term: Long, success: Boolean)

  case class VoteRequested(term: Long, candidateId: String, lastLogIndex: Long, lastLogTerm: Long)
  case class RequestVoteResult(term: Long, voteGranted: Boolean)

  /**
    * To be implemented by users of the module using whatever networking method is relevant for them.
    *
    * @param node identifier of the node at the other end of the RPC
    * @param entriesAppended callback to invoke when another node sends an appendEntries RPC
    * @param voteRequested callback to invoke when another node sends a voteRequested RPC
    * @tparam Command the user command type.
    */
  type RPCFactory[Command] = (String,
                              EntriesToAppend[Command] => Future[AppendEntriesResult],
                              VoteRequested => Future[RequestVoteResult]) => RPC[Command]

  trait RPC[Command] {

    def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult]

    def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult]
  }

  sealed trait NodeEvent

  case object Start extends NodeEvent
  case object ElectionTimeout extends NodeEvent
  case object MajorityVoteReceived extends NodeEvent
  case class NodeWithHigherTermDiscovered(node: String) extends NodeEvent
  case class LeaderDiscovered(node: String) extends NodeEvent

  // Figure 4: Server states.
  class NodeFSM[Command](becomeFollower: NodeEvent => Unit,
                         becomeCandidate: NodeEvent => Unit,
                         becomeLeader: NodeEvent => Unit) {

    private def followerState: Behavior[NodeEvent] = setup { _ =>
      becomeFollower(Start)

      receiveMessage {
        case ElectionTimeout =>
          becomeCandidate(ElectionTimeout)
          candidateState
        case _ =>
          unhandled
      }
    }

    private def candidateState: Behavior[NodeEvent] =
      receiveMessage {
        case ElectionTimeout =>
          becomeCandidate(ElectionTimeout)
          same
        case MajorityVoteReceived =>
          becomeLeader(MajorityVoteReceived)
          leaderState
        case _ =>
          unhandled
      }

    private def leaderState: Behavior[NodeEvent] =
      receiveMessage {
        case e: NodeWithHigherTermDiscovered =>
          becomeFollower(e)
          followerState
        case _ =>
          unhandled
      }

    def stateMachine: Behavior[NodeEvent] = followerState
  }
}
