package io.iohk.cef.net.transport

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import io.iohk.cef.net.SimpleNode2
import io.iohk.cef.net.SimpleNode2.Send

object RLPxNode2 extends App {

  val config = ConfigFactory.parseString(
    """
      |akka {
      |  loglevel = "DEBUG"
      |  actor {
      |    debug {
      |      # enable function of LoggingReceive, which is to log any received message at
      |      # DEBUG level
      |      receive = on
      |    }
      |  }
      |}
    """.stripMargin)

  import SimpleNode2.{Start, Started}

  val start: Behavior[String] = Behaviors.setup {
    context =>

      val aActor = context.spawn(new SimpleNode2("A", 3000, None).server, "NodeA")

      val nodeAStarted: Behavior[Started] = Behaviors.receive {
        (context, message) => message match {
          case Started(nodeAUri) =>

            val bActor = context.spawn(new SimpleNode2("B", 4000, Some(nodeAUri)).server, "NodeB")

            val nodeBStarted: Behavior[Started] = Behaviors.receiveMessage {
              case Started(nodeBUri) =>

                aActor ! Send("Hello Bob, my name is Alice", nodeBUri)

//                bActor ! Send("Hello Alice, my name is Bob", nodeAUri)

                Behavior.same
            }

            bActor ! Start(context.spawn(nodeBStarted, "Node_B_Startup_Listener"))

            Behavior.same
        }
      }

      Behaviors.receive {
        (context, _) =>
          aActor ! Start(context.spawn(nodeAStarted, "Node_A_Startup_Listener"))
          Behavior.ignore
      }
  }

  ActorSystem(start, "main") ! "go"
}
