package io.iohk.cef.consensus
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.language.higherKinds

trait Consensus[F[_], State] {
  def ledgerId: Int

  def process[Header <: BlockHeader](block: Block[State, Header, Transaction[State]]): F[Either[ConsensusError, Unit]]
}
