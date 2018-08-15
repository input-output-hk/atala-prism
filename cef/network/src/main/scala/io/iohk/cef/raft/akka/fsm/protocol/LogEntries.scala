package io.iohk.cef.raft.akka.fsm.protocol

import akka.actor.ActorRef


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

sealed trait Command extends Serializable
case class LogEntries[T <: Command](
  entries: List[Entry[T]],
  committedIndex: Int,
  defaultBatchSize: Int) {

  // log state
  def lastTerm: Term = entries.lastOption map { _.term } getOrElse Term(0)
  def lastIndex: Int = entries.lastOption map { _.index } getOrElse 1
  /**
    * Convinient method for increment
    * Determines index of the next Entry that will be inserted into this log.
    * First entry gets index 1 (not 0, which indicates empty log)
    */
  def nextIndex: Int = entries.size + 1
}

class EmptyLog[T <: Command](defaultBatchSize: Int) extends LogEntries[T](List.empty, 0, defaultBatchSize) {
  override def lastTerm: Term = Term(0)
  override def lastIndex: Int = 1
}

object LogEntries {
  def empty[T <:Command](defaultBatchSize: Int): LogEntries[T] = new EmptyLog[T](defaultBatchSize)
}


