package io.iohk.cef.ledger.storage.mv

import io.iohk.cef.ledger.{Block, BlockHeader}
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.test.DummyTransaction

import scala.collection.JavaConverters._
import java.nio.file.{Files, Path}

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class MVLedgerStorageSpec extends FlatSpec {

  behavior of "MVLedgerStorage"

  it should "push a block" in testStorage { storage =>
    // given
    val ledgerId = "A"
    val block = Block[String, DummyTransaction](BlockHeader(), Seq())
    val blockCodec = NioEncDec[Block[String, DummyTransaction]]

    // when
    storage.push(ledgerId, block)

    // TODO LedgerStorage has no retrieval or iteration mechanism.
    // then
    storage.mvTables.table(ledgerId).values().asScala.toList shouldBe List(blockCodec.encode(block))
  }

  def testStorage(testCode: MVLedgerStorage => Any): Unit = {
    val tempFile: Path = Files.createTempFile("", "")
    val storage = new MVLedgerStorage(tempFile)
    try {
      testCode(storage)
    } finally {
      Files.delete(tempFile)
    }
  }
}
