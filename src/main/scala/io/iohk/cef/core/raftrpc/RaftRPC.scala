package io.iohk.cef.core.raftrpc

import io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft._
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.encoding.nio._
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.{NodeId /*, RequestResponse*/}

import scala.concurrent.Future
import scala.reflect.ClassTag

class RaftRPCFactory[Command: NioEncoder: NioDecoder: ClassTag](
    networkDiscovery: NetworkDiscovery,
    transports: Transports)
    extends RPCFactory[Command] {

  override def apply(
      nodeId: String,
      appendEntriesCallback: raft.EntriesToAppend[Command] => raft.AppendEntriesResult,
      requestVoteCallback: VoteRequested => RequestVoteResult): RaftRPC[Command] =
    new RaftRPC[Command](NodeId(nodeId), appendEntriesCallback, requestVoteCallback, networkDiscovery, transports)
}

class RaftRPC[Command: NioEncoder: NioDecoder: ClassTag](
    nodeId: NodeId,
    appendEntriesCallback: EntriesToAppend[Command] => AppendEntriesResult,
    requestVoteCallback: VoteRequested => RequestVoteResult,
    networkDiscovery: NetworkDiscovery,
    transports: Transports)
    extends RPC[Command] {

//  private val voteHandler = new RequestResponse[VoteRequested, RequestVoteResult](networkDiscovery, transports)
//  private val appendHandler =
//    new RequestResponse[EntriesToAppend[Command], AppendEntriesResult](networkDiscovery, transports)
//  private val clientAppendHandler =
//    new RequestResponse[Seq[Command], Either[Redirect[Command], Unit]](networkDiscovery, transports)

  /**
    * Make an appendEntries RPC call to a raft node.
    *
    * @param entriesToAppend as described in Figure 2, AppendEntries RPC.
    * @return the result from the raft node.
    */
  override def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] = ???
//    appendHandler.sendRequest(nodeId, entriesToAppend)

  /**
    * Make a requestVote RPC call to a raft node.
    *
    * @param voteRequested as described in Figure 2, AppendEntries RPC.
    * @return the result from the raft node.
    */
  override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] = ???
//    voteHandler.sendRequest(nodeId, voteRequested)

  /**
    * This function enables the consensus module to support requests from clients
    * as shown in Figure 1.
    *
    * @param entries the commands which the user wishes to append to the log.
    * @return the response from the raft node.
    */
  override def clientAppendEntries(entries: Seq[Command]): Future[Either[Redirect[Command], Unit]] = ???
//    clientAppendHandler.sendRequest(nodeId, entries)
}
