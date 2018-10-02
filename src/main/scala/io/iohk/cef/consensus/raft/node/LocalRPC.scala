package io.iohk.cef.consensus.raft.node

import io.iohk.cef.consensus.raft._

import scala.concurrent.{ExecutionContext, Future}

class LocalRPC[T](peer: => RaftNode[T])(implicit ec: ExecutionContext) extends RPC[T] {

  override def appendEntries(entriesToAppend: EntriesToAppend[T]): Future[AppendEntriesResult] = {
    Future(peer.appendEntries(entriesToAppend))
  }

  override def requestVote(voteRequested: VoteRequested): Future[RequestVoteResult] =
    Future(peer.requestVote(voteRequested))

  override def clientAppendEntries(entries: Seq[T]): Future[Either[Redirect[T], Unit]] =
    peer.clientAppendEntries(entries)
}
