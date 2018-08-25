package io.iohk.cef.consensus.raft

import io.iohk.cef.consensus.raft.protocol._

trait InitialState {
  this: RaftActor =>


  /** Waits for initial cluster configuration. Step needed before we can start voting for a Leader. */
   lazy val initialConfiguration: StateFunction = {

    case Event(ChangeConfiguration(initialConfig), sd: StateData) =>
      log.info("Applying initial raft cluster configuration. Consists of [{}] nodes: {}",
        initialConfig.members.size,
        initialConfig.members.map(_.path.elements.last).mkString("{", ", ", "}"))
      log.info("Finished init of new Raft member, becoming Follower - {}", sd.self.path)
      goto(Follower) applying WithNewConfigEvent(config = initialConfig)


    // handle initial discovery of nodes, as opposed to initialization via `initialConfig`
    case Event(added: RaftMemberAdded, sd: StateData) =>
      val newMembers = sd.members + added.member

      val initialConfig = ClusterConfiguration(newMembers)

      if (added.keepInitUntil <= newMembers.size) {
        log.info("Discovered the required min. of {} raft cluster members, becoming Follower.", added.keepInitUntil)
        goto(Follower) applying WithNewConfigEvent(config = initialConfig)
      } else {
        // keep waiting for others to be discovered
        log.info("Up to {} discovered raft cluster members, still waiting in Init until {} discovered.", newMembers.size, added.keepInitUntil)
        stay() applying WithNewConfigEvent(config = initialConfig)
      }

    case Event(removed: RaftMemberRemoved, sd: StateData) =>
      val newMembers = sd.config.members - removed.member

      val waitingConfig = ClusterConfiguration(newMembers)

      // keep waiting for others to be discovered
      log.debug("Removed one member, until now discovered {} raft cluster members, still waiting in Init until {} discovered.", newMembers.size, removed.keepInitUntil)
      stay() applying WithNewConfigEvent(config = waitingConfig)

   }


}
