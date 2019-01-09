package io.iohk.cef.transactionservice.raft

import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.raft.RaftConsensus
import io.iohk.cef.consensus.{Consensus, ConsensusError}
import io.iohk.cef.ledger.{Block, Transaction}

import scala.concurrent.{ExecutionContext, Future}

class RaftConsensusInterface[State, Tx <: Transaction[State]](
    val ledgerId: LedgerId,
    raftConsensus: RaftConsensus[Block[State, Tx]]
) extends Consensus[State, Tx] {

  override def process(
      block: Block[State, Tx]
  )(implicit executionContext: ExecutionContext): Future[Either[ConsensusError, Unit]] = {
    raftConsensus.appendEntries(Seq(block)).map(_ => Right(()))
  }
}
