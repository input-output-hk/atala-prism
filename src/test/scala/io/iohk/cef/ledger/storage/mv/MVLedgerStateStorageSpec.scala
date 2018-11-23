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

  it should "update and retrieve a state" in testStorage { storage =>
    // given
    val entry1 = ("A", randomUUID())
    val entry2 = ("B", randomUUID())
    val entry3 = ("C", randomUUID())

    val ledgerState: LedgerState[UUID] = LedgerState(Map(entry1, entry2, entry3))

    // when
    storage.update(LedgerState(Map.empty), ledgerState)
    val slice = storage.slice[UUID](Set("A", "B"))

    // then
    slice shouldBe LedgerState(Map(entry1, entry2))
  }

  def testStorage(testCode: MVLedgerStateStorage => Any): Unit = {
    val tempFile: Path = Files.createTempFile("", "")
    val storage = new MVLedgerStateStorage(tempFile)
    try {
      testCode(storage)
    } finally {
      Files.delete(tempFile)
    }
  }
}
