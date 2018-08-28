package io.iohk.cef.consensus.raft
sealed trait Cmd
case class AppendTestRPC(word: String) extends Cmd
case object GetTestRPCs extends Cmd

class SimpleCommandRaftActor extends RaftActor {
  override type Command = Cmd
  var rpcCmds = Vector[String]()

  /**
    * Use this method to change the actor's internal state.
    * It will be called with whenever a message is committed by the raft cluster.
    *
    * @return the returned value will be sent back to the client issuing the command.
    *         The reply is only sent once, by the current raft leader.
    */
  override def apply: ReplicateStateMachine = {

    case AppendTestRPC(cmd) =>
      rpcCmds = rpcCmds :+ cmd
      log.info(s"Applied command [AppendTestRPC($cmd)], full words is: $rpcCmds")
      cmd

    case GetTestRPCs =>
      log.info("Replying with {}", rpcCmds.toList)
      rpcCmds.toList
  }
}
