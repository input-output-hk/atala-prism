package io.iohk.cef.consensus.raft
import com.typesafe.config.Config

import scala.concurrent.duration.Duration

class RaftConfig(
    val clusterMemberIds: Set[String],
    val electionTimeoutRange: (Duration, Duration),
    val heartbeatTimeoutRange: (Duration, Duration)
)

object RaftConfig {
  def apply(config: Config): RaftConfig = {
    import scala.collection.JavaConverters._
    import io.iohk.cef.utils.JavaTimeConversions._
    new RaftConfig(
      config.getStringList("clusterMemberIds").asScala.toSet,
      (config.getDuration("electionTimeoutRangeStart"), config.getDuration("electionTimeoutRangeEnd")),
      (config.getDuration("heartbeatTimeoutRangeStart"), config.getDuration("heartbeatTimeoutRangeEnd"))
    )
  }
}
