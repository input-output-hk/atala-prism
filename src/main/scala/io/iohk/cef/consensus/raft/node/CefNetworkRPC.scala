package io.iohk.cef.consensus.raft.node
import io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft._
import io.iohk.cef.network.{Network, NodeId}
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.transport.Transports

import scala.concurrent.Future

class CefNetworkRPCFactory[Command](network: Network[Command]) extends RPCFactory[Command] {

  override def apply(
      nodeId: String,
      appendEntriesCallback: raft.EntriesToAppend[Command] => raft.AppendEntriesResult,
      requestVoteCallback: VoteRequested => RequestVoteResult): RPC[Command] =
    new CefNetworkRPC[Command](NodeId(nodeId), appendEntriesCallback, requestVoteCallback)
}

class CefNetworkRPC[Command](
    nodeId: NodeId,
    appendEntriesCallback: EntriesToAppend[Command] => AppendEntriesResult,
    requestVoteCallback: VoteRequested => RequestVoteResult)
    extends RPC[Command] {

  /**
    * Make an appendEntries RPC call to a raft node.
    *
    * @param entriesToAppend as described in Figure 2, AppendEntries RPC.
    * @return the result from the raft node.
    */
  override def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] = ???

  /**
    * Make a requestVote RPC call to a raft node.
    *
    * @param voteRequested as described in Figure 2, AppendEntries RPC.
    * @return the result from the raft node.
    */
  override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] = ???

  /**
    * This function enables the consensus module to support requests from clients
    * as shown in Figure 1.
    *
    * @param entries the commands which the user wishes to append to the log.
    * @return the response from the raft node.
    */
  override def clientAppendEntries(entries: Seq[Command]): Future[Either[Redirect[Command], Unit]] = ???
}
