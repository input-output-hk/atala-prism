package io.iohk.cef.demo

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object RLPxNodeA extends App {

  import SimpleNode2.{Start, Started}

  val start: Behavior[String] = Behaviors.setup {
    context =>

      val aActor = context.spawn(new SimpleNode2("A", 3000, None).server, "NodeA")

      val nodeAStarted: Behavior[Started] = Behaviors.receive {
        (context, message) => message match {
          case Started(nodeAUri) =>
            println("Node A started")
            Behavior.same
        }
      }

      Behaviors.receive {
        (context, _) =>
          aActor ! Start(context.spawn(nodeAStarted, "Node_A_Startup_Listener"))
          Behavior.same
      }
  }

  ActorSystem(start, "main") ! "go"
}
