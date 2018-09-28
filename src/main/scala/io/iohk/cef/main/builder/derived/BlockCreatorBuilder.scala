package io.iohk.cef.main.builder.derived
import akka.actor.Props
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.main.builder.base.{ConsensusBuilder, LedgerConfigBuilder}
import io.iohk.cef.transactionpool.BlockCreator

import scala.concurrent.ExecutionContext

trait BlockCreatorBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: ConsensusBuilder[S, H, T]
    with TransactionPoolBuilder[S, H, T]
    with LedgerConfigBuilder
    with ConsensusBuilder[S, H, T] =>

  def blockCreator(
      implicit executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S]): Props =
    Props(
      new BlockCreator(
        txPoolActorModelInterface,
        consensus,
        ledgerConfig.blockCreatorInitialDelay,
        ledgerConfig.blockCreatorInterval
      ))
}
