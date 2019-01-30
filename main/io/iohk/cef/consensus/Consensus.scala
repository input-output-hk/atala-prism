package io.iohk.cef.consensus
import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.ledger.{Block, Transaction}

import scala.concurrent.{ExecutionContext, Future}

trait Consensus[State, Tx <: Transaction[State]] {
  def ledgerId: LedgerId

  def process(block: Block[State, Tx])(
      implicit executionContext: ExecutionContext
  ): Future[Either[ConsensusError, Unit]]
}
