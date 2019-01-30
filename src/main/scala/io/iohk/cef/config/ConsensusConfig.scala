package io.iohk.cef.config

import io.iohk.cef.consensus.raft.RaftConfig

case class ConsensusConfig(raftConfig: Option[RaftConfig])
