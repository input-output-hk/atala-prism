package io.iohk.cef.main.builder
import akka.util.Timeout
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.network.encoding.array.ArrayCodecs.{ArrayDecoder, ArrayEncoder}
import io.iohk.cef.transactionpool.BlockCreator

import scala.concurrent.ExecutionContext

class BlockCreatorBuilder[S, H <: BlockHeader, T <: Transaction[S]](
    consensusBuilder: ConsensusBuilder[S, H, T],
    txPoolBuilder: TransactionPoolBuilder[S, H, T],
    ledgerConfigBuilder: LedgerConfigBuilder,
    commonTypeAliases: CommonTypeAliases[S, H, T]) {

  import consensusBuilder._
  import txPoolBuilder._
  import ledgerConfigBuilder._
  import commonTypeAliases._

  def blockCreator(
      implicit executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[B],
      sByteStringSerializable: ByteStringSerializable[S],
      timeout: Timeout,
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      lByteStringSerializable: ByteStringSerializable[LogEntry[Block[S, H, T]]],
      arrayEncoder: ArrayEncoder[B],
      arrayDecoder: ArrayDecoder[B],
      arrayLEncoder: ArrayEncoder[LogEntry[B]],
      arrayLDecoder: ArrayDecoder[LogEntry[B]]): Props =
    Props(
      new BlockCreator(
        txPoolActorModelInterface,
        consensus,
        ledgerConfig.blockCreatorInitialDelay,
        ledgerConfig.blockCreatorInterval
      ))
}
