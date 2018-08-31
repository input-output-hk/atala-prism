package io.iohk.cef.consensus.raft
//import io.iohk.cef.consensus.raft.RaftConsensus.LogEntry
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Suite}
import org.scalatest.Matchers._
import io.iohk.cef.network.encoding.array.ArrayCodecs._

import scala.util.Random

case class Command(s: String, i: Int)

class OnDiskPersistentStorageSpec extends FlatSpec with TestSetup {


  behavior of "OnDiskPersistentStorage"

  it should """return state = (0, "") on first usage""" in {
    persistentStorage.state shouldBe (0, "")
  }

  it should "return log = Vector() on first usage" in {
    persistentStorage.log shouldBe Vector()
  }

  it should "update state" in {
    val (term, votedFor) = (1, "leader-id")

    persistentStorage.state(term, votedFor)

    persistentStorage.state shouldBe (term, votedFor)
  }

//  it should "update logs" in {
//    val expectedLog = Vector[LogEntry[Command]](LogEntry(Command("A", 0), 1, 0))
//
//    persistentStorage.set((0, ""), expectedLog)
//
//    persistentStorage.log shouldBe expectedLog
//  }
//
//  it should "support log deletion" in {
//    val initialLog = Vector[LogEntry[Command]](
//      LogEntry(Command("A", 0), 1, 0),
//      LogEntry(Command("B", 0), 1, 1),
//      LogEntry(Command("C", 0), 1, 2))
//
//    val secondaryLog = Vector[LogEntry[Command]](
//      LogEntry(Command("A", 0), 1, 0),
//      LogEntry(Command("B", 0), 1, 1),
//      LogEntry(Command("C", 0), 2, 2))
//
//    persistentStorage.set((0, ""), initialLog)
//    persistentStorage.set((0, ""), secondaryLog)
//
//    persistentStorage.log shouldBe secondaryLog
//  }
//  }
}

trait TestSetup extends BeforeAndAfterAll { this: Suite =>

  val nodeId: String = Random.alphanumeric.take(6).mkString
  val persistentStorage: OnDiskPersistentStorage[Command] = new OnDiskPersistentStorage[Command](nodeId)

  override def afterAll() {
    FileUtils.deleteDirectory(persistentStorage.storageDir.toFile)
  }
}
