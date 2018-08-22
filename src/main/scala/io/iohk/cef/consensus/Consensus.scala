package io.iohk.cef.consensus
import io.iohk.cef.ledger.{Block, Transaction}

trait Consensus[F[_], State] {
  def ledgerId: Int

  def process[Header](block: Block[State, Header, Transaction[State]]): F[Either[ConsensusError, Unit]]
}
