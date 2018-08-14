package io.iohk.cef.raft.akka.fsm.protocol

import akka.actor.ActorRef
import scala.annotation.switch


case class Entry[T](
                     command: T,
                     term: Term,
                     index: Int,
                     client: Option[ActorRef] = None
                   ) {
  assert(index > 0)

  def prevTerm: Term = term.prev

  def prevIndex: Int = index - 1
}


/**
 * @param defaultBatchSize number of commands that can be sent together in one message.
 */
case class LogEntries[Command](
  entries: List[Entry[Command]],
  committedIndex: Int,
  defaultBatchSize: Int) {

  // log state
  def lastTerm: Term = entries.lastOption map { _.term } getOrElse Term(0)
  def lastIndex: Int = entries.lastOption map { _.index } getOrElse 1

}

class EmptyLog[T](defaultBatchSize: Int) extends LogEntries[T](List.empty, 0, defaultBatchSize) {
  override def lastTerm: Term = Term(0)
  override def lastIndex: Int = 1
}

object LogEntries {
  def empty[T](defaultBatchSize: Int): LogEntries[T] = new EmptyLog[T](defaultBatchSize)
}


