package io.iohk.cef.main.builder

import akka.actor.ActorSystem

trait ActorSystemBuilder {
  val actorSystem: ActorSystem
}

trait DefaultActorSystemBuilder extends ActorSystemBuilder {
  override val actorSystem: ActorSystem = ActorSystem("cef-system")
}
