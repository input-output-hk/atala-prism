package io.iohk.cef.main.builder.base
import akka.actor.ActorSystem

trait ActorSystemBuilder {
  val actorSystem: ActorSystem
}

class DefaultActorSystemBuilder extends ActorSystemBuilder {
  override val actorSystem: ActorSystem = ActorSystem()
}
