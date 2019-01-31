package io.iohk.cef.consensus.raft.node
import java.lang.System.getProperty
import java.nio.file.{Files, Path, Paths}

import io.iohk.cef.consensus.raft.{LogEntry, PersistentStorage}
import io.iohk.codecs.nio._
import io.iohk.codecs.nio.auto._
import journal.io.api.Journal.WriteType
import journal.io.api.{Journal, JournalBuilder, Location}

import scala.collection.JavaConverters._
import io.iohk.cef.utils._

class OnDiskPersistentStorage[T: NioCodec](nodeId: String)(implicit codec: NioCodec[LogEntry[T]])
    extends PersistentStorage[T] {

  val storageDir: Path = Paths.get(getProperty("java.io.tmpdir"), "raft", nodeId)
  val logDir: Path = storageDir.resolve("log")
  val stateFile: Path = storageDir.resolve("state")
  val stateCodec: NioCodec[(Int, String)] = NioCodec[(Int, String)]

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
    stateCodec
      .decode(Files.readAllBytes(stateFile).toByteBuffer)
      .getOrElse(throw new IllegalStateException("The state file has become corrupted."))

  private def writeState(term: Int, votedFor: String): Path =
    Files.write(stateFile, stateCodec.encode(term, votedFor).toArray)

  private def readLog: Vector[LogEntry[T]] = {
    journal.redo().asScala.flatMap(location => codec.decode(location.getData.toByteBuffer)).toVector
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
    writes.foreach(write => journal.write(codec.encode(write).toArray, WriteType.SYNC))
  }
}
