package io.iohk.cef.main
import akka.actor.ActorSystem

trait ActorSystemBuilder {
  val actorSystem: ActorSystem
}
