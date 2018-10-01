package io.iohk.cef.main
import akka.util.Timeout
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.core.NodeCore
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.main.builder.base.{CommonTypeAliases, ConsensusBuilder, LedgerConfigBuilder}
import io.iohk.cef.main.builder.derived.{NetworkBuilder, TransactionPoolBuilder}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.network.encoding.nio.NioCodecs._

import scala.concurrent.ExecutionContext

trait NodeCoreBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: NetworkBuilder[S, H, T]
    with LedgerConfigBuilder
    with TransactionPoolBuilder[S, H, T]
    with ConsensusBuilder[S, H, T]
    with CommonTypeAliases[S, H, T] =>

  def nodeCore(
      implicit
      txNetworkEncoder: NioEncoder[ET],
      txNetworkDecoder: NioDecoder[ET],
      blockNetworkEncoder: NioEncoder[EB],
      blockNetworkDecoder: NioDecoder[EB],
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: ByteStringSerializable[B],
      stateyteStringSerializable: ByteStringSerializable[S],
      ebByteStringSerializable: ByteStringSerializable[EB],
      etStringSerializable: ByteStringSerializable[ET],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      lByteStringSerializable: ByteStringSerializable[LogEntry[Block[S, H, T]]]): NodeCore[S, H, T] = new NodeCore(
    Map(ledgerId -> (txPoolFutureInterface, consensus)),
    txNetwork,
    blockNetwork,
    nodeId
  )
}
