package io.iohk.cef.discovery

import java.util.UUID

import akka.actor.{ActorLogging, Props}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.{actor => untyped}
import io.iohk.cef.db.KnownNode
import io.iohk.cef.discovery.DiscoveryProtocol.FindPeers

class DiscoveryProtocolImpl(discoveryManagerProps: untyped.Props) extends DiscoveryProtocol {

  import akka.actor.typed.scaladsl.adapter._

  override def createDiscovery(): Behavior[FindPeers] = Behaviors.setup {
    context =>
      val discoveryManager = context.actorOf(discoveryManagerProps)

      Behaviors.receive {
        (context, message) =>
          message match {
            case fp: FindPeers =>
              val discoveryManagerBridge = context.actorOf(
                DiscoveryManagerBridge.props(discoveryManager),
                s"DiscoveryManagerBridge_${UUID.randomUUID().toString}")
              discoveryManagerBridge ! fp
              Behavior.same
          }
      }
  }

  class DiscoveryManagerBridge(discoveryManager: untyped.ActorRef) extends untyped.Actor with ActorLogging {

    override def receive: Receive = {
      case FindPeers(replyTo) =>
        discoveryManager ! GetDiscoveredNodes()
        context.become(awaitingDiscoveryReply(replyTo))
    }

    def awaitingDiscoveryReply(replyTo: ActorRef[Set[KnownNode]]): Receive = {
      case DiscoveredNodes(nodes) =>
        replyTo ! nodes
        context.stop(self)
    }
  }

  object DiscoveryManagerBridge {
    def props(discoveryManager: untyped.ActorRef): Props = Props(new DiscoveryManagerBridge(discoveryManager))
  }
}
