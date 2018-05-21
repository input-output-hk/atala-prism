package io.iohk.cef.net.transport

import java.net.URI

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import io.iohk.cef.net.SimpleNode2
import io.iohk.cef.net.SimpleNode2.Send

object RLPxNodeB extends App {

  val config = ConfigFactory.parseString(
    """
      |akka {
      |  loglevel = "DEBUG"
      |  actor {
      |    debug {
      |      # enable function of LoggingReceive, which is to log any received message at
      |      # DEBUG level
      |      receive = on
      |      unhandled = on
      |      trace-logging = on
      |    }
      |  }
      |}
    """.stripMargin)

  import SimpleNode2.{Start, Started}

  val start: Behavior[String] = Behaviors.setup {
    context =>

      val bActor = context.spawn(new SimpleNode2("B", 4000, None).server, "NodeB")

      val nodeBStarted: Behavior[Started] = Behaviors.receive {
        (context, message) => message match {
          case Started(_) =>
            println("Node B started")

            bActor ! Send("Hello, Alice! I'm Bob.",
              new URI("enode://7089d553b732ec4b60cf84819f8cdbf1c22e8ef95d3b9d936936157e89f45249a87863c4d51b0cfa110f0d3c3f8d9fc9cc3f9bce2aec84fbcf3bbb39f1c3bca7@127.0.0.1:3000"))

            Behavior.same
        }
      }

      Behaviors.receive {
        (context, _) =>
          bActor ! Start(context.spawn(nodeBStarted, "Node_B_Startup_Listener"))
          Behavior.same
      }
  }

  ActorSystem(start, "main") ! "go"
}
