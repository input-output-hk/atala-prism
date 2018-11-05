package io.iohk.cef.main.builder

import io.iohk.cef.codecs.nio._
import io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.node.OnDiskPersistentStorage
import io.iohk.cef.consensus.raft.{PersistentStorage, RPCFactory, RaftConfig}
import io.iohk.cef.core.raftrpc.RaftRPCFactory
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage

import scala.concurrent.ExecutionContext

abstract class RaftConsensusConfigBuilder[C] {
  val raftConfig: RaftConfig
  def storage(
      implicit
      commandSerializable: NioEncDec[C]): PersistentStorage[C]
  def rpcFactory(
      implicit serializable: NioEncDec[DiscoveryWireMessage],
      commandSerializable: NioEncDec[C],
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
      commandSerializable: NioEncDec[B]): raft.PersistentStorage[B] = {
    new OnDiskPersistentStorage[B](nodeIdStr)
  }
  override val raftConfig: RaftConfig = RaftConfig(config.getConfig("consensus.raft"))
  override def rpcFactory(
      implicit serializable: NioEncDec[DiscoveryWireMessage],
      commandSerializable: NioEncDec[B],
      executionContext: ExecutionContext): raft.RPCFactory[B] = {
    new RaftRPCFactory[B](networkDiscovery, transports)
  }
}
