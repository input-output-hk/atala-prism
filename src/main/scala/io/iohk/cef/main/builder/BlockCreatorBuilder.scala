package io.iohk.cef.main.builder
import akka.actor.Props
import akka.util.Timeout
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.transactionpool.BlockCreator

import scala.concurrent.ExecutionContext

trait BlockCreatorBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: ConsensusBuilder[S, H, T]
    with TransactionPoolBuilder[S, H, T]
    with LedgerConfigBuilder
    with ConsensusBuilder[S, H, T]
    with CommonTypeAliases[S, H, T] =>

  def blockCreator(
      implicit executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[B],
      sByteStringSerializable: ByteStringSerializable[S],
      timeout: Timeout,
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      lByteStringSerializable: ByteStringSerializable[LogEntry[Block[S, H, T]]]): Props =
    Props(
      new BlockCreator(
        txPoolActorModelInterface,
        consensus,
        ledgerConfig.blockCreatorInitialDelay,
        ledgerConfig.blockCreatorInterval
      ))
}
