package io.iohk.cef.ledger.storage.mv
import java.nio.file.{Files, Path}
import java.util.UUID
import java.util.UUID.randomUUID

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.codecs.nio.auto._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class MVLedgerStateStorageSpec extends FlatSpec {

  behavior of "MVLedgerStateStorage"

  it should "retrieve an initial state" in testStorage { storage =>
    // when
    val slice = storage.slice(Set("non-existent-key"))

    // then
    slice shouldBe LedgerState()
  }

  it should "update and retrieve a state" in testStorage { storage =>
    // given
    val entry1 = ("A", randomUUID())
    val entry2 = ("B", randomUUID())
    val entry3 = ("C", randomUUID())
    val ledgerState: LedgerState[UUID] = LedgerState(Map(entry1, entry2, entry3))

    // when
    storage.update(ledgerState)
    val slice1 = storage.slice(Set("A", "B"))
    val slice2 = storage.slice(Set("C"))
    val slice3 = storage.slice(Set("D"))

    // then
    slice1 shouldBe LedgerState(Map(entry1, entry2))
    slice2 shouldBe LedgerState(Map(entry3))
    slice3 shouldBe LedgerState()
  }

  def testStorage(testCode: MVLedgerStateStorage[UUID] => Any): Unit = {
    val tempFile: Path = Files.createTempFile("", "")
    val storage = new MVLedgerStateStorage[UUID]("ledger-id", tempFile)
    try {
      testCode(storage)
    } finally {
      Files.delete(tempFile)
    }
  }
}
