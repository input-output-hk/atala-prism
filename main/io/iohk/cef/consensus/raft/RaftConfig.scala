package io.iohk.cef.consensus.raft
import scala.concurrent.duration.Duration

case class RaftConfig(
    nodeId: String,
    clusterMemberIds: Seq[String],
    electionTimeoutRange: (Duration, Duration),
    heartbeatTimeoutRange: (Duration, Duration)
)
