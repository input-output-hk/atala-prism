package io.iohk.cef.ledger.storage.mv

import io.iohk.cef.ledger.{Block, BlockHeader}
import io.iohk.codecs.nio.auto._
import io.iohk.cef.test.DummyTransaction

import scala.collection.JavaConverters._
import java.nio.file.{Files, Path}

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class MVLedgerStorageSpec extends FlatSpec {

  behavior of "MVLedgerStorage"

  it should "push a block" in testStorage { storage =>
    // given
    val block = Block[String, DummyTransaction](BlockHeader(), Seq())

    // when
    storage.push(block)

    // TODO LedgerStorage has no retrieval or iteration mechanism.
    // then
    storage.mvTable.table.values().asScala.toList shouldBe List(block)
  }

  def testStorage(testCode: MVLedgerStorage[String, DummyTransaction] => Any): Unit = {
    val tempFile: Path = Files.createTempFile("", "")
    val storage = new MVLedgerStorage[String, DummyTransaction]("A", tempFile)
    try {
      testCode(storage)
    } finally {
      Files.delete(tempFile)
    }
  }
}
