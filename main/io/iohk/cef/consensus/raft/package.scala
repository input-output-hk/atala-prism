package io.iohk.cef.consensus

import io.iohk.cef.consensus.raft.node._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import io.iohk.codecs.nio._

package object raft {

  /**
    * Create and configure a raft node.
    *
    * @param nodeId A unique identifier within the cluster.
    * @param clusterMemberIds the ids of the other cluster members.
    * @param rpcFactory as described above.
    * @param electionTimeoutRange range of values for the election timeout (see raft paper).
    * @param heartbeatTimeoutRange range of values for the heartbeat timeout (see raft paper).
    * @param stateMachine the user 'state machine' to which committed log entries will be applied.
    * @param persistentStorage as described above.
    * @param ec an execution context for Futures.
    * @tparam Command the user command type.
    * @return a raft node implementation.
    */
  def raftNode[Command: NioCodec](
      nodeId: String,
      clusterMemberIds: Seq[String],
      rpcFactory: RPCFactory[Command],
      electionTimeoutRange: (Duration, Duration),
      heartbeatTimeoutRange: (Duration, Duration),
      stateMachine: Command => Unit,
      persistentStorage: PersistentStorage[Command]
  )(implicit ec: ExecutionContext): RaftNodeInterface[Command] =
    new RaftNode[Command](
      nodeId,
      clusterMemberIds,
      rpcFactory,
      electionTimeoutRange,
      heartbeatTimeoutRange,
      stateMachine,
      persistentStorage
    )

}
