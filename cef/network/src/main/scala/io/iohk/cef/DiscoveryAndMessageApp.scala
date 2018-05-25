package io.iohk.cef

import java.net.URI
import java.time.Instant

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.{actor => untyped}
import io.iohk.cef.net.SimpleNode2
import io.iohk.cef.net.SimpleNode2.{Send, Start, Started}
import io.iohk.cef.network.{Capabilities, Node}
import io.iohk.cef.db.KnownNode
import io.iohk.cef.discovery.{DiscoveredNodes, GetDiscoveredNodes}

import scala.concurrent.duration._

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
        import akka.actor.typed.scaladsl.adapter._

        val nodeActor: ActorRef[SimpleNode2.NodeCommand] =
          context.spawn(new SimpleNode2(nodeName, port, None).server, "NodeActor")

        val startupListener: Behavior[Started] = Behaviors.receiveMessage {
          case Started(nodeUri) =>

            context.actorOf(SayHelloActor.props(nodeUri, bootstrapNodes, nodeActor))

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

class SayHelloActor(nodeUri: URI, bootstrapNodeUris: Set[URI],
                    nodeActor: ActorRef[SimpleNode2.NodeCommand]) extends untyped.Actor with untyped.ActorLogging {

  val discoveryActor: untyped.ActorRef =
    DiscoveryActor(nodeUri, bootstrapNodeUris, Capabilities(1))(context)

  rescheduleTick()

  private def sayHelloTo(peers: Set[KnownNode]): Unit = {
    val livingPeers = peers.filter(notDead).map(_.node) // TODO how to get discovery to do this bit?

    log.debug(s"It's message time! My peers are $livingPeers")

    livingPeers.foreach(greet)
  }

  private def greet(peer: Node): Unit =
    nodeActor ! Send(greeting(peer.toUri, nodeUri), peer.toUri)

  private def greeting(you: URI, me: URI): String =
    s"Hello, $you! I'm $me"

  private def notDead(knownNode: KnownNode): Boolean =
    knownNode.lastSeen.plusSeconds(10).isAfter(Instant.now)

  override def receive: Receive = {
    case DiscoveredNodes(nodes) =>
      sayHelloTo(nodes)
    case "DiscoveryTick" =>
      discoveryActor ! GetDiscoveredNodes()
      rescheduleTick()
  }

  private def rescheduleTick(): Unit = {
    import context.dispatcher
    context.system.scheduler.scheduleOnce(5 seconds, self, "DiscoveryTick")
  }
}

object SayHelloActor {
  def props(nodeUri: URI, bootstrapNodeUris: Set[URI],
            nodeActor: ActorRef[SimpleNode2.NodeCommand]): untyped.Props =
    untyped.Props(new SayHelloActor(nodeUri, bootstrapNodeUris, nodeActor))
}


