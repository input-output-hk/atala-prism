package io.iohk.cef.main
import akka.actor.Props
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.transactionpool.BlockCreator

import scala.concurrent.ExecutionContext

trait BlockCreatorBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: ConsensusBuilder[S, H, T]
    with TransactionPoolBuilder[S, H, T]
    with ConfigurationBuilder
    with ConsensusBuilder[S, H, T] =>

  def blockCreator(
      implicit executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]]): Props =
    Props(
      new BlockCreator(
        txPoolActorModelInterface,
        consensus,
        blockCreatorInitialDelay,
        blockCreatorInterval
      ))
}
