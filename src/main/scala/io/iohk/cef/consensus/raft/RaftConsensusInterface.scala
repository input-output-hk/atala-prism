package io.iohk.cef.consensus.raft

import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.{Consensus, ConsensusError}
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.concurrent.{ExecutionContext, Future}

class RaftConsensusInterface[State, Header <: BlockHeader, Tx <: Transaction[State]](
    val ledgerId: LedgerId,
    raftConsensus: RaftConsensus[Block[State, Header, Tx]])
    extends Consensus[State, Header, Tx] {

  override def process(block: Block[State, Header, Tx])(
      implicit executionContext: ExecutionContext): Future[Either[ConsensusError, Unit]] = {
    raftConsensus.appendEntries(Seq(block)).map(_ => Right(()))
  }
}
