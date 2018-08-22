package io.iohk.cef.consensus.raft.protocol

import akka.actor.ActorRef



sealed trait ClusterConfiguration {
  def members: Set[ActorRef]
}


object ClusterConfiguration {
  def apply(members: Iterable[ActorRef]): ClusterConfiguration =
    StableClusterConfiguration (members.toSet)
  def apply(members: ActorRef*): ClusterConfiguration =
    StableClusterConfiguration(members.toSet)
}


case class StableClusterConfiguration( members: Set[ActorRef]) extends ClusterConfiguration {

  override def toString: String = s"StableRaftConfiguration(${members.map(_.path.elements.last)})"

}