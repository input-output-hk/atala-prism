package io.iohk.cef.raft.akka.fsm.config

import akka.actor.{ExtendedActorSystem, ExtensionId, ExtensionIdProvider}

object RaftConfiguration extends ExtensionId[RaftConfig] with ExtensionIdProvider {

  def lookup() = RaftConfiguration

  def createExtension(system: ExtendedActorSystem): RaftConfig = new RaftConfig(system.settings.config)
}
