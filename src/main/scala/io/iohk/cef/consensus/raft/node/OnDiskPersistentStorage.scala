package io.iohk.cef.consensus.raft.node
import java.lang.System.getProperty
import java.nio.file.{Files, Path, Paths}

import io.iohk.cef.consensus.raft.{LogEntry, PersistentStorage}
import io.iohk.cef.codecs.nio._
import journal.io.api.Journal.WriteType
import journal.io.api.{Journal, JournalBuilder, Location}

import scala.collection.JavaConverters._
import io.iohk.cef.utils._

class OnDiskPersistentStorage[T: NioEncoder: NioDecoder](nodeId: String)(
    implicit enc: NioEncoder[LogEntry[T]],
    dec: NioDecoder[LogEntry[T]])
    extends PersistentStorage[T] {

  val storageDir: Path = Paths.get(getProperty("java.io.tmpdir"), "raft", nodeId)
  val logDir: Path = storageDir.resolve("log")
  val stateFile: Path = storageDir.resolve("state")
  val stateEncoder = { import auto._; NioEncoder[(Int, String)] }
  val stateDecoder = { import auto._; NioDecoder[(Int, String)] }

  initializeStorage()

  private val journal: Journal = JournalBuilder.of(logDir.toFile).open

  override def state: (Int, String) = readState

  override def log: Vector[LogEntry[T]] = {
    readLog
  }

  override def state(currentTerm: Int, votedFor: String): Unit = {
    writeState(currentTerm, votedFor)
  }

  private def readState: (Int, String) =
    stateDecoder
      .decode(Files.readAllBytes(stateFile).toByteBuffer)
      .getOrElse(throw new IllegalStateException("The state file has become corrupted."))

  private def writeState(term: Int, votedFor: String): Path =
    Files.write(stateFile, stateEncoder.encode(term, votedFor).toArray)

  private def readLog: Vector[LogEntry[T]] = {
    journal.redo().asScala.flatMap(location => dec.decode(location.getData.toByteBuffer)).toVector
  }

  private def initializeStorage(): Unit = {
    if (Files.notExists(storageDir))
      Files.createDirectories(logDir)

    if (Files.notExists(stateFile))
      writeState(0, "")
  }

  private[raft] def truncate(): Unit = {
    journal.close()
    journal.truncate()
    journal.open()
  }

  override def log(deletes: Int, writes: Seq[LogEntry[T]]): Unit = {
    journal.undo().asScala.takeRight(deletes).foreach((entry: Location) => journal.delete(entry))
    writes.foreach(write => journal.write(enc.encode(write).toArray, WriteType.SYNC))
  }
}
