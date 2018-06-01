package io.iohk.cef.utils

import java.util.UUID

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

object UtilityBehaviors {
  
  def ignore[T](ctx: ActorContext[_]): ActorRef[T] =
    ctx.spawn(Behaviors.ignore, s"ignore_${UUID.randomUUID().toString}")
}
