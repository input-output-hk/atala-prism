package io.iohk.cef.consensus.raft

class InMemoryPersistentStorage[T](var logEntries: Vector[LogEntry[T]], var currentTerm: Int, var votedFor: String)
    extends PersistentStorage[T] {

  override def state: (Int, String) = (currentTerm, votedFor)

  override def log: Vector[LogEntry[T]] = logEntries

  override def state(currentTerm: Int, votedFor: String): Unit = {
    this.currentTerm = currentTerm
    this.votedFor = votedFor
  }
  override def log(deletes: Int, writes: Seq[LogEntry[T]]): Unit = {
    logEntries = logEntries.dropRight(deletes) ++ writes
  }
}
