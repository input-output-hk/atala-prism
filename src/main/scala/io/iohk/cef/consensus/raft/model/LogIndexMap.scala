package io.iohk.cef.consensus.raft.model

import akka.actor.ActorRef
import io.iohk.cef.consensus.raft.protocol.ClusterConfiguration

import scala.annotation.tailrec

/**
 * Implements convenience methods for state on leaders
 */
case class LogIndexMap private (private var backing: Map[ActorRef, Int], private val initializeWith: Int) {

  val Zero = 0
  def decrementFor(member: ActorRef): Int = {
    val value = backing(member) - 1
    backing = backing.updated(member, value)
    value
  }


  def put(member: ActorRef, value: Int): Unit = {
    backing = backing.updated(member, value)
  }

  /** Only put the new `value` if it is __greater than__ the already present value in the map */
  def putIfGreater(member: ActorRef, value: Int): Int =
    putIf(member, _ < _, value)

  /** @param compare (old, new) => should put? */
  def putIf(member: ActorRef, compare: (Int, Int) => Boolean, value: Int): Int = {
    val oldValue = valueFor(member)

    if (compare(oldValue, value)) {
      put(member, value)
      value
    } else {
      oldValue
    }
  }

  @tailrec final def valueFor(member: ActorRef): Int = backing.get(member) match {
    case None =>
      backing = backing.updated(member, initializeWith)
      valueFor(member)
    case Some(value) =>
      value
  }

  def consensusForIndex(config: ClusterConfiguration): Int =  {
    indexOnMajority(config.members)
  }

  private def indexOnMajority(include: Set[ActorRef]): Int = {
    // find the match index i that has the
    // following property:
    // a quorum [N / 2 + 1] of the nodes has match index >= i
    // We first sort the match indices
    val sortedMatchIndices = backing
      .filterKeys(include)
      .values
      .toList
      .sorted

    if (sortedMatchIndices.isEmpty) {
      Zero
    }else{
      sortedMatchIndices(LogIndexMap.ceiling(include.size, 2) - 1)
    }

  }



}

object LogIndexMap {
  def initialize(members: Set[ActorRef], initializeWith: Int): LogIndexMap =
    new LogIndexMap(Map(members.toList.map(_ -> initializeWith): _*), initializeWith)

  def ceiling(numerator: Int, divisor: Int): Int = {
    if (numerator % divisor == 0) numerator / divisor else (numerator / divisor) + 1
  }
}
