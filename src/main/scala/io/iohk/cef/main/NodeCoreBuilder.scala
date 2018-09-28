package io.iohk.cef.main
import io.iohk.cef.ledger.{BlockHeader, Transaction}
import io.iohk.cef.main.builder.base.{ConsensusBuilder, LedgerConfigBuilder}
import io.iohk.cef.main.builder.derived.NetworkBuilder
trait NodeCoreBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: NetworkBuilder[S, H, T] with LedgerConfigBuilder with ConsensusBuilder[S, H, T] =>

//  val nodeCore: NodeCore[S, H, T] = new NodeCore(
//    consensusMap,
//    txNetwork,
//    blockNetwork,
//    nodeId
//  )
}
