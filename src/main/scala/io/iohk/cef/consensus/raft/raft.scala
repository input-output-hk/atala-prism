package io.iohk.cef.consensus.raft

import java.util.UUID

import io.iohk.cef.consensus.raft.node._

import scala.concurrent.{ExecutionContext, Future}

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
    * @param clientAppendEntriesCallback when a (remote) consensus module makes an inbound clientAppendEntries call,
    *                                   implementers should invoke this function.
    * @return an RPC implementation
    */
  def apply(
      nodeId: String,
      appendEntriesCallback: EntriesToAppend[Command] => AppendEntriesResult,
      requestVoteCallback: VoteRequested => RequestVoteResult,
      clientAppendEntriesCallback: Seq[Command] => Future[Either[Redirect[Command], Unit]]): RPC[Command]
}

/**
  * Redirect provides a way for nodes who receive append requests from clients
  * to redirect to their leader. Used by RPC below.
  */
case class Redirect[Command](nodeId: String)

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

  private def appendEntries(leader: String, entries: Seq[Command]): Future[Unit] = {
    val leaderRPC = raftNode.getRPC(leader)
    leaderRPC.clientAppendEntries(entries).flatMap {
      case Left(Redirect(nextLeaderId)) =>
        appendEntries(nextLeaderId, entries)
      case Right(()) =>
        Future.unit
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
    writes: IndexedSeq[LogEntry[Command]],
    leaderId: String,
    uuid: UUID) {
  val log = new VirtualVector(baseLog, deletes, writes)
}

trait RaftNodeInterface[Command] {
  def getLeader: String
  def getRPC(nodeId: String): RPC[Command]
}
