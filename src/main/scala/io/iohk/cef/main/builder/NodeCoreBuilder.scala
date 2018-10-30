package io.iohk.cef.main.builder
import akka.util.Timeout
import io.iohk.cef.codecs.array.ArrayCodecs._
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.core.NodeCore
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage

import scala.concurrent.ExecutionContext

class NodeCoreBuilder[S, H <: BlockHeader, T <: Transaction[S]](
    networkBuilder: NetworkBuilder[S, H, T],
    ledgerConfigBuilder: LedgerConfigBuilder,
    transactionPoolBuilder: TransactionPoolBuilder[S, H, T],
    consensusBuilder: ConsensusBuilder[S, H, T],
    commonTypeAliases: CommonTypeAliases[S, H, T]
) {
  import commonTypeAliases._
  import consensusBuilder._
  import ledgerConfigBuilder._
  import networkBuilder._
  import transactionPoolBuilder._

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
