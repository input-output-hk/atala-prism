package io.iohk.cef.raft.akka.fsm

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.persistence.fsm.PersistentFSM.{CurrentState, SubscribeTransitionCallBack, Transition, UnsubscribeTransitionCallBack}
import io.iohk.cef.raft.akka.fsm.MonitoringActor._
import io.iohk.cef.raft.akka.fsm.protocol._

import scala.collection._

object MonitoringActor {
  case class Subscribe(members: Seq[ActorRef])
  case class AddMember(member: ActorRef)
  case object GetLeaders
  case class Leaders(leaders: Seq[ActorRef])
  case object GetCandidates
  case class Candidates(candidates: Seq[ActorRef])
  case object GetFollowers
  case class Followers(followers: Seq[ActorRef])
  case object Unsubscribe
  case class RemoveMember(member: ActorRef)

}


class MonitoringActor extends Actor with ActorLogging {

  var stateOfMembers = mutable.Map.empty[ActorRef, RaftState  @unchecked]

  var members = Seq.empty[ActorRef]

  override def receive: Receive = {
    case Subscribe(clusterMembers) =>
      members = clusterMembers
      stateOfMembers = mutable.Map.empty[ActorRef, RaftState  @unchecked]
      members.foreach { member =>
        member ! SubscribeTransitionCallBack(self)
      }
    case msg: CurrentState[RaftState  @unchecked] => stateOfMembers += (msg.fsmRef -> msg.state)
    case msg: Transition[RaftState  @unchecked] => stateOfMembers += (msg.fsmRef -> msg.to)
    case AddMember(ref) =>
      ref ! SubscribeTransitionCallBack(self)
      stateOfMembers += (ref -> Init)
    case RemoveMember(ref) =>
      if(stateOfMembers.contains(ref)) {
        ref ! UnsubscribeTransitionCallBack(self)
        stateOfMembers -= ref
      }
    case GetLeaders => sender ! Leaders(stateOfMembers.filter(_._2 == Leader).keySet.toList)
    case GetCandidates => sender ! Candidates(stateOfMembers.filter(_._2 == Candidate).keySet.toList)
    case GetFollowers => sender ! Followers(stateOfMembers.filter(_._2 == Follower).keySet.toList)
    case Unsubscribe =>
      members.foreach { member =>
        member ! UnsubscribeTransitionCallBack(self)
      }
  }
}
