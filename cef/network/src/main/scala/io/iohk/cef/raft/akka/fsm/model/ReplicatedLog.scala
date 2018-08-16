package io.iohk.cef.raft.akka.fsm.model

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

sealed trait Command extends Product with  Serializable

case class ReplicatedLog[T <: Command](
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

  /**
    * @param fromIncluding index from which to start the slice (including the entry at that index)
    *
    * N.B. log entries are 1-indexed.
    */
  def entriesBatchFrom(fromIncluding: Int, howMany: Int = 5): List[Entry[T]] = {
    val adjusted = fromIncluding - 1
    assert(adjusted >= 0)
    val toSend = entries.slice(adjusted, adjusted + howMany)
    toSend.headOption match {
      case Some(head) =>
        val batchTerm = head.term
        toSend.takeWhile(_.term == batchTerm) // we only batch commands grouped by their term

      case None =>
        List.empty
    }
  }

  /**
    * Performs the "consistency check", which checks if the data that we just got from the
    */
  def containsMatchingEntry(otherPrevTerm: Term, otherPrevIndex: Int): Boolean =
    (otherPrevTerm == Term(0) && otherPrevIndex == 1) ||
      (entries.isDefinedAt(otherPrevIndex - 1) && entries(otherPrevIndex - 1).term == otherPrevTerm && lastIndex == otherPrevIndex)


  def termAt(index: Int): Term = {
    if (index <= 0) {
      Term(0)
    }
    else if (!entries.exists(_.index == index)) {
      throw new IllegalArgumentException(s"Unable to find log entry at index $index.")
    }
    else entries.find(_.index == index).get.term
  }
}

class EmptyReplicatedLog[T <: Command](defaultBatchSize: Int) extends ReplicatedLog[T](List.empty, 0, defaultBatchSize) {
  override def lastTerm: Term = Term(0)
  override def lastIndex: Int = 1
}

object ReplicatedLog {
  def empty[T <:Command](defaultBatchSize: Int): ReplicatedLog[T] = new EmptyReplicatedLog[T](defaultBatchSize)
}


