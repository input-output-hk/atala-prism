package io.iohk.cef.consensus

import io.iohk.cef.consensus.raft.node._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

package object raft {

  /**
    * To be implemented by users of the module to provide their desired RPC implementation.
    * RPCFactory provides the mechanism by which the RaftNode on one host
    * can create RPC instances to talk to the other nodes in the cluster.
    */
  trait RPCFactory[Command] {

    /**
      * @param nodeId This is the nodeId of a node in the raft cluster.
      * @param appendEntriesCallback when a (remote) raft node makes an inbound appendEntries call,
      *                              implementers should invoke this function.
      * @param requestVoteCallback when a (remote) raft node makes an inbound requestVote call,
      *                            implementers should invoke this function.
      * @return
      */
    def apply(
        nodeId: String,
        appendEntriesCallback: EntriesToAppend[Command] => AppendEntriesResult,
        requestVoteCallback: VoteRequested => RequestVoteResult): RPC[Command]
  }

  /**
    * Redirect provides a way for nodes who receive append requests from clients
    * to redirect to their leader. Used by RPC below.
    */
  case class Redirect[Command](leaderRPC: RPC[Command])

  /**
    * To be implemented by users of the module using whatever network technology suits them.
    *
    * @tparam Command the user command type.
    */
  trait RPC[Command] {

    /**
      * Make an appendEntries RPC call to a raft node.
      *
      * @param entriesToAppend as described in Figure 2, AppendEntries RPC.
      * @return the result from the raft node.
      */
    def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult]

    /**
      * Make a requestVote RPC call to a raft node.
      *
      * @param voteRequested as described in Figure 2, AppendEntries RPC.
      * @return the result from the raft node.
      */
    def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult]

    /**
      * This function enables the consensus module to support requests from clients
      * as shown in Figure 1.
      *
      * @param entries the commands which the user wishes to append to the log.
      * @return the response from the raft node.
      */
    def clientAppendEntries(entries: Seq[Command]): Future[Either[Redirect[Command], Unit]]
  }

  /**
    * AppendEntries RPC Log replication request made by leaders to followers
    */
  case class EntriesToAppend[Command](
      term: Int,
      leaderId: String,
      prevLogIndex: Int,
      prevLogTerm: Int,
      entries: Seq[LogEntry[Command]],
      leaderCommitIndex: Int)

  /**
    * AppendEntries RPC response
    */
  case class AppendEntriesResult(term: Int, success: Boolean)

  /**
    * RequestVote RPC request made by candidates for other nodes.
    */
  case class VoteRequested(term: Int, candidateId: String, lastLogIndex: Int, lastLogTerm: Int)

  /**
    * RequestVote RPC response
    */
  case class RequestVoteResult(term: Int, voteGranted: Boolean)

  /**
    * LogEntry provides a wrapper around user commands with metadata essential to the working
    * or the algorithm.
    */
  case class LogEntry[Command](command: Command, term: Int, index: Int)

  /**
    * To be implemented by users of the module using whatever persistence technology suits them.
    *
    * @tparam Command the user command type.
    */
  trait PersistentStorage[Command] {

    /**
      * Return the stored term and votedFor fields.
      * If none are available, implementations should return (0, "")
      */
    def state: (Int, String)

    /**
      * A read only view of the log.
      * Raft scrolls backwards through the log so implementations can
      * scroll from the tail.
      */
    def log: IndexedSeq[LogEntry[Command]]

    def state(currentTerm: Int, votedFor: String): Unit

    def log(deletes: Int, writes: Seq[LogEntry[Command]]): Unit
  }

  /**
    * Can be implemented by users of the module if they wish to provide a timer implementation.
    */
  trait RaftTimerFactory {

    /**
      * @param timeoutHandler this enables the Raft node to pass a function which the timer should
      *                       call upon timeout.
      * @return a RaftTimer implementation.
      */
    def apply(timeoutHandler: () => Unit): RaftTimer
  }

  /**
    * A RaftTimer should implement a randomized, always on timer.
    * That is,
    * it should schedule a random timeout.
    * when that timeout occurs it should invoke the timeout handler
    * it should then reschedule another random timeout.
    */
  trait RaftTimer {

    /**
      * Reset causes the timer to cancel the current timeout and reschedule
      * another one (without calling the timeoutHandler).
      */
    def reset(): Unit
  }

  /**
    * A JDK based timer with random timeouts between 150 and 300 ms,
    * generally suitable for LANs.
    */
  // FIXME: remove magic numbers.
  val defaultElectionTimerFactory: RaftTimerFactory =
    timeoutHandler => new BouncyTimer(150 millis, 300 millis)(timeoutHandler)

  /**
    * A JDK based timer with a fixed, 75 ms timeout.
    */
  // FIXME: remove magic numbers.
  val defaultHeartbeatTimerFactory: RaftTimerFactory =
    timeoutHandler => new BouncyTimer(75 millis, 75 millis)(timeoutHandler)

  /**
    * From Figure 1. The Consensus module ensures client requests go to the leader.
    * This is the top-level interface used by clients.
    */
  class RaftConsensus[Command](raftNode: RaftNodeInterface[Command])(implicit ec: ExecutionContext) {
    // Sec 5.1
    // The leader handles all client requests
    // (if a client contacts a follower, the follower redirects it to the leader)
    def appendEntries(entries: Seq[Command]): Future[Unit] = {
      appendEntries(raftNode.getLeader, entries)
    }

    private def appendEntries(leaderRpc: RPC[Command], entries: Seq[Command]): Future[Unit] = {
      leaderRpc.clientAppendEntries(entries).flatMap {
        case Left(Redirect(nextLeaderRPC)) =>
          appendEntries(nextLeaderRPC, entries)
        case Right(()) =>
          Future(())
      }
    }
  }

  /**
    * Raft server state. See figure 2 for descriptions of fields.
    */
  case class RaftState[Command](
      role: NodeRole[Command],
      commonVolatileState: CommonVolatileState[Command],
      leaderVolatileState: LeaderVolatileState,
      persistentState: (Int, String),
      baseLog: IndexedSeq[LogEntry[Command]],
      deletes: Int,
      writes: Seq[LogEntry[Command]],
      leaderId: String) {
    val log = new VirtualVector(baseLog, deletes, writes)
  }

  trait RaftNodeInterface[Command] {
    def getLeader: RPC[Command]
  }

  /**
    * Create and configure a raft node.
    * @param nodeId A unique identifier within the cluster.
    * @param clusterMemberIds the ids of the other cluster members.
    * @param rpcFactory as described above.
    * @param electionTimerFactory as described above.
    * @param heartbeatTimerFactory as described above.
    * @param stateMachine the user 'state machine' to which committed log entries will be applied.
    * @param persistentStorage as described above.
    * @param ec an execution context for Futures.
    * @tparam Command the user command type.
    * @return a raft node implementation.
    */
  def raftNode[Command](
      nodeId: String,
      clusterMemberIds: Seq[String],
      rpcFactory: RPCFactory[Command],
      electionTimerFactory: RaftTimerFactory = defaultElectionTimerFactory,
      heartbeatTimerFactory: RaftTimerFactory = defaultHeartbeatTimerFactory,
      stateMachine: Command => Unit,
      persistentStorage: PersistentStorage[Command])(implicit ec: ExecutionContext): RaftNodeInterface[Command] =
    new RaftNode[Command](
      nodeId,
      clusterMemberIds,
      rpcFactory,
      electionTimerFactory,
      heartbeatTimerFactory,
      stateMachine,
      persistentStorage)
}
