package io.iohk.cef.main.builder
import akka.util.Timeout
import io.iohk.cef.core.NodeCore
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable, Transaction}
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
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: ByteStringSerializable[B],
      stateyteStringSerializable: ByteStringSerializable[S],
      txStringSerializable: ByteStringSerializable[T],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]): NodeCore[S, H, T] = new NodeCore(
    Map(ledgerConfig.id -> (txPoolFutureInterface, consensus)),
    txNetwork,
    blockNetwork,
    nodeId
  )
}
