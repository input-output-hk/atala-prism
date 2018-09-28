package io.iohk.cef.main.builder.base
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.main.builder.helpers.RaftConsensusConfig

trait ConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  val consensus: Consensus[S, H, T]
}

class RaftConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: LedgerConfigBuilder =>
  type B = Block[S, H, T]
  require(consensusConfig.isInstanceOf[RaftConsensusConfig[B]])


}
