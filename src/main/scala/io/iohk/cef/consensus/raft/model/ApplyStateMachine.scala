package io.iohk.cef.consensus.raft.model

trait ApplyStateMachine {
  type ReplicateStateMachine = PartialFunction[Any, Any]

  /**
    * Use this method to change the actor's internal state.
    * It will be called with whenever a message is committed by the raft cluster.
    *
    * @return the returned value will be sent back to the client issuing the command.
    *         The reply is only sent once, by the current raft leader.
    */
  def apply: ReplicateStateMachine



}
