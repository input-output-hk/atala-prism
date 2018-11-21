package io.iohk.cef.data.storage.mv

import java.nio.file.{Files, Path}

import io.iohk.cef.data.DataItem
import io.iohk.cef.codecs.nio.auto._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.EitherValues._
import scala.util.Random

class MVTableStorageSpec extends FlatSpec {

  behavior of "MVTableStorage"

  it should "insert and select a data item" in testFile { storageFile =>
    // given
    val storage = new MVTableStorage(storageFile)
    val expectedDataItem: DataItem[String] = DataItem("A", Random.nextString(28), Seq(), Seq())

    // when
    storage.insert("foo", expectedDataItem)
    val actualDataItem = storage.selectSingle[String]("foo", "A").right.value

    // then
    actualDataItem shouldBe expectedDataItem
  }

  def testFile(testCode: Path => Any): Unit = {

    val tempFile: Path = Files.createTempFile("", "")

    println(tempFile)
    try {
      testCode(tempFile)
    } finally {
      Files.delete(tempFile)
    }
  }
}
