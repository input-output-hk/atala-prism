package io.iohk.cef.main
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.{BlockHeader, Transaction}

trait ConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]] {

  val consensus: Consensus[S, H, T]
}
