package io.iohk.cef.net.transport

import java.net.URI

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import io.iohk.cef.net.SimpleNode2
import io.iohk.cef.net.SimpleNode2.Send
import io.iohk.cef.utils.PortFinder

object RLPxNodeB extends App {

  import SimpleNode2.{Start, Started}

  val start: Behavior[String] = Behaviors.setup {
    context =>

      val bActor = context.spawn(new SimpleNode2("B", PortFinder.aPort, None).server, "NodeB")

      val nodeBStarted: Behavior[Started] = Behaviors.receiveMessage {
        case Started(_) =>
          println("Node B started")

          bActor ! Send("Hello, Alice! I'm Bob.",
            new URI("enode://011866dfb208191b57bd9eabfcd1967fbc5dd5db7c11e620028cc730fec6a088409a90583ae37a07c83ed76a4c8b242d6c601eb6e5fe48cfdfce0cea63465e28@127.0.0.1:3000"))


          Behavior.same
      }

      Behaviors.receive {
        (context, _) =>
          bActor ! Start(context.spawn(nodeBStarted, "Node_B_Startup_Listener"))
          Behavior.same
      }
  }

  ActorSystem(start, "main") ! "go"
}
