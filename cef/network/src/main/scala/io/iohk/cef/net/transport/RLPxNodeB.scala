package io.iohk.cef.net.transport

import java.net.{InetAddress, URI}

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import io.iohk.cef.net.SimpleNode2
import io.iohk.cef.net.SimpleNode2.Send
import javax.net.ServerSocketFactory

import scala.util.Random

object RLPxNodeB extends App {

  object PortFinder {
    private def isAvailable(port: Int): Boolean = {
      try {
        ServerSocketFactory.getDefault.createServerSocket(
          port, 1, InetAddress.getByName("localhost")).close()
        true
      }
      catch {
        case _: Throwable => false
      }
    }

    private def randomPort = 1024 + Random.nextInt(65535 - 1024 + 1)

    def aPort: Int = {
      val port = randomPort
      if (isAvailable(port))
        port
      else
        aPort
    }
  }

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
