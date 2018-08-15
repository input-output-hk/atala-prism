package io.iohk.cef.raft.akka.fsm.protocol

import akka.actor.ActorRef



sealed trait ClusterConfiguration {
  def members: Set[ActorRef]
}

 object ClusterConfiguration extends ClusterConfiguration{
  override def members: Set[ActorRef] = ???
}

