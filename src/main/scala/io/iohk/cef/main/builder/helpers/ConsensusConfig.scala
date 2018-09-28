package io.iohk.cef.main.builder.helpers
import io.iohk.cef.consensus.raft.PersistentStorage

import scala.concurrent.duration.Duration

sealed trait ConsensusConfig

class RaftConsensusConfig[Command](
    val nodeConfig: Map[String, PersistentStorage[Command]],
    val electionTimeoutRange: (Duration, Duration),
    val heartbeatTimeoutRange: (Duration, Duration)
                         ) extends ConsensusConfig
