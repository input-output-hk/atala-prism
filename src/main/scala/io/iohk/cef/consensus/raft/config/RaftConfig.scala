package io.iohk.cef.consensus.raft.config

import java.util.concurrent.TimeUnit

import akka.actor.Extension
import com.typesafe.config.Config

import scala.concurrent.duration._

class RaftConfig (config: Config) extends Extension {

  val raftConfig = config.getConfig("akka.raft")

  val defaultAppendEntriesBatchSize = raftConfig.getInt("default-append-entries-batch-size")

  val electionTimeoutMin = raftConfig.getDuration("election-timeout.min", TimeUnit.MILLISECONDS).millis
  val electionTimeoutMax = raftConfig.getDuration("election-timeout.max", TimeUnit.MILLISECONDS).millis

  val heartbeatInterval = raftConfig.getDuration("heartbeat-interval", TimeUnit.MILLISECONDS).millis

  val publishTestEvents = raftConfig.getBoolean("publish-test-events")


}
