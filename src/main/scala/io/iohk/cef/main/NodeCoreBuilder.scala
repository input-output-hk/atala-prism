package io.iohk.cef.main
import akka.util.Timeout
import io.iohk.cef.core.NodeCore
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.main.builder.base.{CommonTypeAliases, ConsensusBuilder, LedgerConfigBuilder}
import io.iohk.cef.main.builder.derived.{NetworkBuilder, TransactionPoolBuilder}
import io.iohk.cef.network.discovery.DiscoveryWireMessage

import scala.concurrent.ExecutionContext

trait NodeCoreBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: NetworkBuilder[S, H, T]
    with LedgerConfigBuilder
    with TransactionPoolBuilder[S, H, T]
    with ConsensusBuilder[S, H, T]
    with CommonTypeAliases[S, H, T] =>

  import EncoderDecoderSimplificationImplicits._

  def nodeCore(
      implicit
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: ByteStringSerializable[B],
      stateyteStringSerializable: ByteStringSerializable[S],
      txStringSerializable: ByteStringSerializable[T],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]): NodeCore[S, H, T] = new NodeCore(
    Map(ledgerId -> (txPoolFutureInterface, consensus)),
    txNetwork,
    blockNetwork,
    nodeId
  )
}
