package io.iohk.cef.consensus.raft
import java.lang.System.getProperty
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, Paths}

import io.iohk.cef.consensus.raft.RaftConsensus.{LogEntry, PersistentStorage}
import io.iohk.cef.network.encoding.array.ArrayCodecs._
import journal.io.api.Journal.WriteType
import journal.io.api.{Journal, JournalBuilder}

import scala.collection.JavaConverters._

class OnDiskPersistentStorage[T: ArrayEncoder: ArrayDecoder](nodeId: String)
                                                            (implicit enc: ArrayEncoder[LogEntry[T]],
                                                             dec: ArrayDecoder[LogEntry[T]]) extends PersistentStorage[T] {

  val storageDir: Path = Paths.get(getProperty("java.io.tmpdir"), nodeId)
  val logDir: Path = storageDir.resolve("log")
  val stateFile: Path = storageDir.resolve("state")

  initializeStorage()

  private val journal: Journal = JournalBuilder.of(logDir.toFile).open

  override def state: (Int, String) = readState

  override def log: Vector[LogEntry[T]] = {
    readLog
  }

  override def state(currentTerm: Int, votedFor: String): Unit = {
    writeState(currentTerm, votedFor)
  }

  private def readState: (Int, String) = {
    val s = new String(Files.readAllBytes(stateFile), UTF_8)
    val split = s.split(',')
    (split(0).toInt, if (split.size == 2) split(1) else "")
  }

  private def writeState(term: Int, votedFor: String): Path =
    Files.write(stateFile, s"$term,$votedFor".getBytes(UTF_8))

  private def writeLog(log: Vector[LogEntry[T]]) = {
    log.foreach(entry => journal.write(enc.encode(entry), WriteType.SYNC))
  }

  private def readLog: Vector[LogEntry[T]] = {
    journal.redo().asScala.flatMap(location => dec.decode(location.getData)).toVector
  }

  private def initializeStorage(): Unit = {
    if (Files.notExists(storageDir))
      Files.createDirectories(logDir)

    if (Files.notExists(stateFile))
      writeState(0, "")
  }

  override def log(deletes: Int,
                   writes: Seq[LogEntry[T]]): Unit =
    ???
}
