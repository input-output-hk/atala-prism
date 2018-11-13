package io.iohk.cef.consensus
import io.iohk.cef.consensus.raft.RaftConfig

case class ConsensusConfig(raftConfig: Option[RaftConfig])
