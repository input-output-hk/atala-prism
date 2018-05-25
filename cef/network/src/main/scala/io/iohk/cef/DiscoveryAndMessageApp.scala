package io.iohk.cef

import java.net.URI
import java.time.Instant

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Logger}
import io.iohk.cef.db.KnownNode
import io.iohk.cef.discovery.DiscoveryProtocol.FindPeers
import io.iohk.cef.discovery.DiscoveryProtocolImpl
import io.iohk.cef.net.SimpleNode2
import io.iohk.cef.net.SimpleNode2.{Send, Start, Started}
import io.iohk.cef.network.{Capabilities, Node}

object DiscoveryAndMessageApp {

  def main(args: Array[String]): Unit = {

    def printUsageAndExit(): Unit = {
      println("""Usage: DiscoveryAndMessageApp --node-name <name> --bootstrap-node-uris <comma sep uri list>""")
      System.exit(1)
    }

    if (args.length == 0) {
      printUsageAndExit()
    }

    val argMap = args.sliding(2, 2).toList.collect {
      case Array("--node-name", nodeName: String) => "node-name" -> nodeName
      case Array("--port", port: String) => "port" -> port
      case Array("--bootstrap-node-uris", bootStrapNodes: String) => "bootstrap-node-uris" -> bootStrapNodes
      case Array("--bootstrap-node-uris") => "bootstrap-node-uris" -> ""
    }.toMap

    val nodeName = argMap("node-name")
    val port = argMap("port").toInt
    val bootstrapNodes = argMap("bootstrap-node-uris").split("\\s*,\\s*").filterNot(_.isEmpty).map(new URI(_)).toSet

    val start: Behavior[String] = Behaviors.setup {
      context =>

        val nodeActor: ActorRef[SimpleNode2.NodeCommand] =
          context.spawn(new SimpleNode2(nodeName, port, None).server, "NodeActor")

        val startupListener: Behavior[Started] = Behaviors.receiveMessage {
          case Started(nodeUri) =>

            val discoveryProtocol = new DiscoveryProtocolImpl(DiscoveryActor.props(nodeUri, bootstrapNodes, Capabilities(1)))

            val discoveryActor = context.spawn(discoveryProtocol.createDiscovery(), "DiscoveryActor")

            context.spawn(new Greeter(nodeUri, nodeActor, discoveryActor).behavior, "PeerGreeter")

            Behavior.ignore
        }

        Behaviors.receive {
          (context, _) =>
            nodeActor ! Start(context.spawn(startupListener, "Node_Startup_Listener"))
            Behavior.same
        }
    }

    ActorSystem(start, "main") ! "go"
  }
}

class Greeter(nodeUri: URI, nodeActor: ActorRef[SimpleNode2.NodeCommand], discoveryActor: ActorRef[FindPeers]) {

  import scala.concurrent.duration._

  val behavior: Behavior[String] = Behaviors.withTimers(timer => tick(timer))

  private val tock: Behavior[Set[KnownNode]] = Behaviors.receive((context, message) => message match {
    case s =>
      sayHelloTo(s, context.log)
      Behavior.same
  })

  private def tick(timer: TimerScheduler[String]): Behavior[String] = Behaviors.setup { context =>

    val tockActor = context.spawn(tock, s"GreeterTock")

    timer.startPeriodicTimer("greeter_timer", "DiscoveryTick", 5 seconds)

    Behaviors.receiveMessage(_ => {
      discoveryActor ! FindPeers(tockActor)
      Behavior.same
    })
  }


  private def sayHelloTo(peers: Set[KnownNode], log: Logger): Unit = {
    val livingPeers = peers.filter(notDead).map(_.node) // TODO how to get discovery to do this bit?

    log.debug(s"It's message time! My peers are $livingPeers")

    livingPeers.foreach(greet)
  }

  private def notDead(knownNode: KnownNode): Boolean =
    knownNode.lastSeen.plusSeconds(10).isAfter(Instant.now)

  private def greet(peer: Node): Unit =
    nodeActor ! Send(greeting(peer.toUri, nodeUri), peer.toUri)

  private def greeting(you: URI, me: URI): String =
    s"Hello, $you! I'm $me"
}

