package io.iohk.cef.transactionservice.raft

import io.iohk.cef.consensus.raft._
import io.iohk.network.discovery.NetworkDiscovery
import io.iohk.codecs.nio.NioCodec
import io.iohk.network.transport.Transports
import io.iohk.network.{NodeId, RequestResponse}
import io.iohk.codecs.nio.auto._

import scala.reflect.runtime.universe._
import scala.concurrent.{ExecutionContext, Future}

class RaftRPCFactory[Command: NioCodec: TypeTag](networkDiscovery: NetworkDiscovery, transports: Transports)(
    implicit ec: ExecutionContext
) extends RPCFactory[Command] {

  override def apply(
      nodeId: String,
      appendEntriesCallback: EntriesToAppend[Command] => AppendEntriesResult,
      requestVoteCallback: VoteRequested => RequestVoteResult,
      clientAppendEntriesCallback: Seq[Command] => Future[Either[Redirect[Command], Unit]]
  ): RaftRPC[Command] =
    new RaftRPC[Command](
      NodeId(nodeId),
      appendEntriesCallback,
      requestVoteCallback,
      clientAppendEntriesCallback,
      networkDiscovery,
      transports
    )
}

class RaftRPC[Command: NioCodec: TypeTag](
    nodeId: NodeId,
    appendEntriesCallback: EntriesToAppend[Command] => AppendEntriesResult,
    requestVoteCallback: VoteRequested => RequestVoteResult,
    clientAppendEntriesCallback: Seq[Command] => Future[Either[Redirect[Command], Unit]],
    networkDiscovery: NetworkDiscovery,
    transports: Transports
)(implicit ec: ExecutionContext)
    extends RPC[Command] {

  private val voteHandler =
    new RequestResponse[VoteRequested, RequestVoteResult](networkDiscovery, transports)

  private val appendHandler =
    new RequestResponse[EntriesToAppend[Command], AppendEntriesResult](networkDiscovery, transports)

  private val clientAppendHandler =
    new RequestResponse[Seq[Command], Either[Redirect[Command], Unit]](networkDiscovery, transports)

  appendHandler.handleRequest(entriesToAppend => appendEntriesCallback(entriesToAppend))
  voteHandler.handleRequest(voteRequested => requestVoteCallback(voteRequested))
  clientAppendHandler.handleFutureRequest(clientEntries => clientAppendEntriesCallback(clientEntries))

  override def appendEntries(entriesToAppend: EntriesToAppend[Command]): Future[AppendEntriesResult] =
    appendHandler.sendRequest(nodeId, entriesToAppend)

  override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] =
    voteHandler.sendRequest(nodeId, voteRequested)

  override def clientAppendEntries(entries: Seq[Command]): Future[Either[Redirect[Command], Unit]] =
    clientAppendHandler.sendRequest(nodeId, entries)
}
