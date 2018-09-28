package io.iohk.cef.main.builder.base
import io.iohk.cef.consensus.raft.{PersistentStorage, RPCFactory}

import scala.concurrent.duration.Duration

trait ConsensusConfigBuilder {}

trait RaftConsensusConfigBuilder[Command] extends ConsensusConfigBuilder {
  val nodeId: String
  val storage: PersistentStorage[Command]
  val clusterMemberIds: Seq[String]
  val electionTimeoutRange: (Duration, Duration)
  val heartbeatTimeoutRange: (Duration, Duration)
  val rpcFactory: RPCFactory[Command]
}
