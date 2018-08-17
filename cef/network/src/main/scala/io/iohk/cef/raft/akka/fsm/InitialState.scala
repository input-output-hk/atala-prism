package io.iohk.cef.raft.akka.fsm

import io.iohk.cef.raft.akka.fsm.protocol._

trait InitialState {
  this: RaftActor =>


  /** Waits for initial cluster configuration. Step needed before we can start voting for a Leader. */
   lazy val initialConfiguration: StateFunction = {

    case Event(ChangeConfiguration(initConfig), _) =>
      log.info("Applying initial raft cluster configuration. Consists of [{}] nodes: {}",
        initConfig.members.size,
        initConfig.members.map(_.path.elements.last).mkString("{", ", ", "}"))
      goto(Follower)

   }


}
