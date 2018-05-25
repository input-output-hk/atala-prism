package io.iohk.cef.discovery

import akka.actor.typed.{ActorRef, Behavior}
import io.iohk.cef.db.KnownNode
import io.iohk.cef.discovery.DiscoveryProtocol.FindPeers

trait DiscoveryProtocol {
  def createDiscovery(): Behavior[FindPeers]
}

object DiscoveryProtocol {
  case class FindPeers(replyTo: ActorRef[Set[KnownNode]])
}
