package io.iohk.cef.main.builder
import akka.util.Timeout
import io.iohk.cef.codecs.nio._
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.transactionpool.BlockCreator

import scala.concurrent.ExecutionContext

class BlockCreatorBuilder[S, H <: BlockHeader, T <: Transaction[S]](
    consensusBuilder: ConsensusBuilder[S, H, T],
    txPoolBuilder: TransactionPoolBuilder[S, H, T],
    ledgerConfigBuilder: LedgerConfigBuilder,
    commonTypeAliases: CommonTypeAliases[S, H, T]) {

  import commonTypeAliases._
  import consensusBuilder._
  import ledgerConfigBuilder._
  import txPoolBuilder._

  def blockCreator(
      implicit executionContext: ExecutionContext,
      byteStringSerializable: NioEncDec[B],
      sNioEncDec: NioEncDec[S],
      timeout: Timeout,
      dNioEncDec: NioEncDec[DiscoveryWireMessage],
      lNioEncDec: NioEncDec[LogEntry[Block[S, H, T]]]): BlockCreator[S, H, T] =
    new BlockCreator(
      txPoolFutureInterface,
      consensus,
      ledgerConfig.blockCreatorInitialDelay,
      ledgerConfig.blockCreatorInterval
    )
}
