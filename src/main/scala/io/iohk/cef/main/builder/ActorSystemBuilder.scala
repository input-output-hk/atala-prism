package io.iohk.cef.main.builder

import akka.actor.ActorSystem

class DefaultActorSystemBuilder {
  val actorSystem: ActorSystem = ActorSystem("cef-system")
}
