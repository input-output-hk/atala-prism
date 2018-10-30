package io.iohk.cef.main.builder

import io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.node.OnDiskPersistentStorage
import io.iohk.cef.consensus.raft.{LogEntry, PersistentStorage, RPCFactory, RaftConfig}
import io.iohk.cef.core.raftrpc.RaftRPCFactory
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.codecs.array.ArrayCodecs._

import scala.concurrent.ExecutionContext

abstract class RaftConsensusConfigBuilder[C] {
  val raftConfig: RaftConfig
  def storage(
      implicit
      commandSerializable: ByteStringSerializable[C],
      arrayEncoder: ArrayEncoder[C],
      arrayDecoder: ArrayDecoder[C],
      arrayLEncoder: ArrayEncoder[LogEntry[C]],
      arrayLDecoder: ArrayDecoder[LogEntry[C]]): PersistentStorage[C]
  def rpcFactory(
      implicit serializable: ByteStringSerializable[DiscoveryWireMessage],
      commandSerializable: ByteStringSerializable[C],
      executionContext: ExecutionContext): RPCFactory[C]
}

class DefaultRaftConsensusConfigBuilder[S, H <: BlockHeader, T <: Transaction[S]](
    defaultConfig: DefaultLedgerConfig,
    configReaderBuilder: ConfigReaderBuilder)
    extends RaftConsensusConfigBuilder[Block[S, H, T]] {

  type BB = Block[S, H, T]

  import configReaderBuilder._
  import defaultConfig._
  override def storage(
      implicit
      commandSerializable: ByteStringSerializable[BB],
      arrayEncoder: ArrayEncoder[BB],
      arrayDecoder: ArrayDecoder[BB],
      arrayLEncoder: ArrayEncoder[LogEntry[BB]],
      arrayLDecoder: ArrayDecoder[LogEntry[BB]]): raft.PersistentStorage[BB] = {
    new OnDiskPersistentStorage[BB](nodeIdStr)
  }
  override val raftConfig: RaftConfig = RaftConfig(config.getConfig("consensus.raft"))
  override def rpcFactory(
      implicit serializable: ByteStringSerializable[DiscoveryWireMessage],
      commandSerializable: ByteStringSerializable[BB],
      executionContext: ExecutionContext): raft.RPCFactory[BB] = {
    new RaftRPCFactory[BB](networkDiscovery, transports)
  }
}
