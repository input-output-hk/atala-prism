package io.iohk.cef.consensus.raft.node
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.encoding.array.ArrayCodecs._
import org.apache.commons.io.FileUtils
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen._
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Suite}

import scala.collection.immutable
import scala.util.Random

case class Command(s: String, i: Int)

class OnDiskPersistentStorageSpec extends FlatSpec with TestSetup {

  private val genLogEntry = for {
    s <- arbitrary[String]
    i <- arbitrary[Int]
    term <- arbitrary[Int]
    index <- arbitrary[Int]
  } yield LogEntry(Command(s, i), term, index)

  private val genState = arbitrary[(Int, String)]

  private val genDeletesAndCommands = for {
    commands: immutable.Seq[LogEntry[Command]] <- listOf(genLogEntry)
    deletes <- choose(0, commands.size)
  } yield {
    (deletes, commands)
  }

  behavior of "OnDiskPersistentStorage"

  it should """return state = (0, "") on first usage""" in {
    persistentStorage.state shouldBe (0, "")
  }

  it should "return log = Vector() on first usage" in {
    persistentStorage.log shouldBe Vector()
  }

  it should "update state" in forAll(genState) { state =>
    val (term, votedFor) = state

    persistentStorage.state(term, votedFor)

    persistentStorage.state shouldBe (term, votedFor)
  }

  it should "append to logs" in forAll(listOf(genLogEntry)) { commands =>
    persistentStorage.log(deletes = 0, writes = commands)

    persistentStorage.log.takeRight(commands.size) shouldBe commands
  }

  it should "delete from logs" in forAll(listOf(genLogEntry)) { commands =>
    persistentStorage.log(deletes = 0, writes = commands)

    persistentStorage.log.takeRight(commands.size) shouldBe commands

    persistentStorage.log(deletes = commands.size, writes = Seq())

    persistentStorage.log shouldBe IndexedSeq()
  }

  it should "do nothing if deletes are applied to an empty log" in {
    persistentStorage.log(deletes = 10, writes = Seq())

    persistentStorage.log shouldBe IndexedSeq()
  }

  it should "process deletes before writes" in forAll(genDeletesAndCommands) {
    case (deletes, commands) =>
      // log is initially empty so no deletes to process
      persistentStorage.log(deletes = deletes, writes = commands)

      persistentStorage.log shouldBe commands

      persistentStorage.log(deletes = deletes, writes = Seq())

      persistentStorage.log.size shouldBe (commands.size - deletes)

      persistentStorage.log(deletes = commands.size - deletes, writes = Seq()) // clear entries
  }
}

trait TestSetup extends BeforeAndAfterEach with BeforeAndAfterAll { this: Suite =>

  val nodeId: String = Random.alphanumeric.take(6).mkString
  val persistentStorage: OnDiskPersistentStorage[Command] = new OnDiskPersistentStorage[Command](nodeId)

  override protected def afterEach(): Unit = {
    persistentStorage.truncate()
  }

  override def afterAll() {
    FileUtils.deleteDirectory(persistentStorage.storageDir.toFile)
  }
}
