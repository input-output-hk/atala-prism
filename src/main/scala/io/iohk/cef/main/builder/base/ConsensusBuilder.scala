package io.iohk.cef.main.builder.base
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.consensus.raft.PersistentStorage
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

trait ConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]] {

  val consensus: Consensus[S, H, T]
}

class RaftNodeConfig[Command](
    val storage: PersistentStorage[Command]
)

class RaftConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]](
    clusterIds: Map[String, RaftNodeConfig[Block[S, H, T]]],
) {}
