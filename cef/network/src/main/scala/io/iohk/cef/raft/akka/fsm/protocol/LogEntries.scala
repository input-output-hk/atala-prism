package io.iohk.cef.raft.akka.fsm.protocol

import io.iohk.cef.raft.LogEntry

case class LogEntries(entries: List[LogEntry],
                      committedIndex: Int) {

  def lastTerm:Term = ???
  def lastIndex:Int= ???

}

object LogEntries extends LogEntries(entries = List.empty[LogEntry],committedIndex = 0)