package io.iohk.cef.main.builder
import akka.util.Timeout
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.core.NodeCore
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.network.encoding.array.ArrayCodecs.{ArrayDecoder, ArrayEncoder}
import io.iohk.cef.network.encoding.nio.NioCodecs._

import scala.concurrent.ExecutionContext

class NodeCoreBuilder[S, H <: BlockHeader, T <: Transaction[S]](
    networkBuilder: NetworkBuilder[S, H, T],
    ledgerConfigBuilder: LedgerConfigBuilder,
    transactionPoolBuilder: TransactionPoolBuilder[S, H, T],
    consensusBuilder: ConsensusBuilder[S, H, T],
    commonTypeAliases: CommonTypeAliases[S, H, T]
) {
  import networkBuilder._
  import commonTypeAliases._
  import ledgerConfigBuilder._
  import transactionPoolBuilder._
  import consensusBuilder._

  def nodeCore(
      implicit
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: ByteStringSerializable[B],
      stateyteStringSerializable: ByteStringSerializable[S],
      txStringSerializable: ByteStringSerializable[T],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      arrayEncoder: ArrayEncoder[B],
      arrayDecoder: ArrayDecoder[B],
      arrayLEncoder: ArrayEncoder[LogEntry[B]],
      arrayLDecoder: ArrayDecoder[LogEntry[B]]): NodeCore[S, H, T] = new NodeCore(
    Map(ledgerConfig.id -> (txPoolFutureInterface, consensus)),
    txNetwork,
    blockNetwork,
    nodeId
  )
}
