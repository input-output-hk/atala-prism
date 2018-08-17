package io.iohk.cef.raft.akka.fsm.model

import akka.actor.ActorRef

import scala.annotation.tailrec

/**
 * Implements convenience methods for state on leaders
 */
case class LogIndexMap private (private var backing: Map[ActorRef, Int], private val initializeWith: Int) {

  def decrementFor(member: ActorRef): Int = {
    val value = backing(member) - 1
    backing = backing.updated(member, value)
    value
  }


  def put(member: ActorRef, value: Int): Unit = {
    backing = backing.updated(member, value)
  }


  @tailrec final def valueFor(member: ActorRef): Int = backing.get(member) match {
    case None =>
      backing = backing.updated(member, initializeWith)
      valueFor(member)
    case Some(value) =>
      value
  }
}

object LogIndexMap {
  def initialize(members: Set[ActorRef], initializeWith: Int): LogIndexMap =
    new LogIndexMap(Map(members.toList.map(_ -> initializeWith): _*), initializeWith)

}
