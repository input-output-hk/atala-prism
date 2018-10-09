package io.iohk.cef.main.builder

import io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.node.OnDiskPersistentStorage
import io.iohk.cef.consensus.raft.{PersistentStorage, RPCFactory, RaftConfig}
import io.iohk.cef.core.raftrpc.RaftRPCFactory
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.network.discovery.DiscoveryWireMessage

import scala.concurrent.ExecutionContext

trait ConsensusConfigBuilder {}

trait RaftConsensusConfigBuilder[Command] extends ConsensusConfigBuilder {
  val raftConfig: RaftConfig
  def storage(implicit commandSerializable: ByteStringSerializable[Command]): PersistentStorage[Command]
  def rpcFactory(
      implicit serializable: ByteStringSerializable[DiscoveryWireMessage],
      commandSerializable: ByteStringSerializable[Command],
      executionContext: ExecutionContext): RPCFactory[Command]
}

trait DefaultRaftConsensusConfigBuilder[Command] extends RaftConsensusConfigBuilder[Command] {
  self: DefaultLedgerConfig with ConfigReaderBuilder =>
  import io.iohk.cef.encoding.array.ArrayCodecs._
  override def storage(
      implicit
      commandSerializable: ByteStringSerializable[Command]): raft.PersistentStorage[Command] = {
    new OnDiskPersistentStorage[Command](nodeIdStr)
  }
  override val raftConfig: RaftConfig = RaftConfig(config.getConfig("consensus.raft"))
  override def rpcFactory(
      implicit serializable: ByteStringSerializable[DiscoveryWireMessage],
      commandSerializable: ByteStringSerializable[Command],
      executionContext: ExecutionContext): raft.RPCFactory[Command] = {
    new RaftRPCFactory[Command](networkDiscovery, transports)
  }
}
