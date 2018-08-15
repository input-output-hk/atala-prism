package io.iohk.cef.raft.akka.fsm

import io.iohk.cef.raft.akka.fsm.protocol.{ClusterConfiguration, _}

trait InitialState {
  this: RaftActor =>


  /** Waits for initial cluster configuration. Step needed before we can start voting for a Leader. */
   lazy val initialConfiguration: StateFunction = {

    case Event(ClusterConfiguration, _) =>
      log.info("Applying initial raft cluster configuration. Consists of [{}] nodes: {}",
        ClusterConfiguration.members.size,
        ClusterConfiguration.members.map(_.path.elements.last).mkString("{", ", ", "}"))
      goto(Follower)

   }


}
