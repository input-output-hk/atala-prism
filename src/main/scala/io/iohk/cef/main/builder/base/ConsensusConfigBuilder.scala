package io.iohk.cef.main.builder.base
import io.iohk.cef.consensus.raft.{PersistentStorage, RPCFactory, RaftConfig}
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
