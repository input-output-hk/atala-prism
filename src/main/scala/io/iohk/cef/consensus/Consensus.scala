package io.iohk.cef.consensus
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.concurrent.Future

trait Consensus[State, Header <: BlockHeader, Tx <: Transaction[State]] {
  def ledgerId: LedgerId

  def process(block: Block[State, Header, Tx]): Future[Either[ConsensusError, Unit]]
}
