package io.iohk.cef.transactionservice.raft

import io.iohk.cef.consensus.Consensus
import io.iohk.cef.consensus.raft.RaftConsensus
import io.iohk.cef.ledger._
import monix.execution.Scheduler.Implicits.global
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

class RaftConsensusInterface[State, Tx <: Transaction[State]](
    override val ledgerId: LedgerId,
    raftConsensus: RaftConsensus[Block[State, Tx]],
    proposedBlocksObservable: ProposedBlocksObservable[State, Tx],
    appliedBlocksObserver: AppliedBlocksObserver[State, Tx]
) extends Consensus[State, Tx] {

  private val logger = LoggerFactory.getLogger(this.getClass)

  proposedBlocksObservable.foreach { proposedBlock =>
    raftConsensus
      .appendEntries(Seq(proposedBlock))
      .onComplete {
        case Failure(exception) =>
          logger.error("Failed to append block", exception)

        case Success(_) =>
          logger.info(s"Block applied successfully = $proposedBlock")
          val _ = appliedBlocksObserver.onNext(proposedBlock)
      }
  }
}
