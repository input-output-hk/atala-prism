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

  type B = Block[S, H, T]

  import configReaderBuilder._
  import defaultConfig._
  override def storage(
      implicit
      commandSerializable: ByteStringSerializable[B],
      arrayEncoder: ArrayEncoder[B],
      arrayDecoder: ArrayDecoder[B],
      arrayLEncoder: ArrayEncoder[LogEntry[B]],
      arrayLDecoder: ArrayDecoder[LogEntry[B]]): raft.PersistentStorage[B] = {
    new OnDiskPersistentStorage[B](nodeIdStr)
  }
  override val raftConfig: RaftConfig = RaftConfig(config.getConfig("consensus.raft"))
  override def rpcFactory(
      implicit serializable: ByteStringSerializable[DiscoveryWireMessage],
      commandSerializable: ByteStringSerializable[B],
      executionContext: ExecutionContext): raft.RPCFactory[B] = {
    new RaftRPCFactory[B](networkDiscovery, transports)
  }
}
