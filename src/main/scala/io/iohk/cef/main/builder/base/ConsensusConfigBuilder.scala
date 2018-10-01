package io.iohk.cef.main.builder.base
import io.iohk.cef.consensus.raft.{PersistentStorage, RPCFactory}
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.network.discovery.DiscoveryWireMessage

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

trait ConsensusConfigBuilder {}

trait RaftConsensusConfigBuilder[Command] extends ConsensusConfigBuilder {
  def storage(
      implicit commandSerializable: ByteStringSerializable[Command]): PersistentStorage[Command]
  val clusterMemberIds: Seq[String]
  val electionTimeoutRange: (Duration, Duration)
  val heartbeatTimeoutRange: (Duration, Duration)
  def rpcFactory(
      implicit serializable: ByteStringSerializable[DiscoveryWireMessage],
      commandSerializable: ByteStringSerializable[Command],
      executionContext: ExecutionContext): RPCFactory[Command]
}
