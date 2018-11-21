package io.iohk.cef.data.storage.mv

import java.nio.file.{Files, Path}

import io.iohk.cef.data.DataItem
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.data.error.DataItemNotFound
import io.iohk.cef.data.query.Query.NoPredicateQuery
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.EitherValues._

import scala.util.Random

class MVTableStorageSpec extends FlatSpec {

  behavior of "MVTableStorage"

  it should "insert and select a data item" in testStorage { storage =>
    // given
    val expectedDataItem = DataItem("A", Random.nextString(28), Seq(), Seq())

    // when
    storage.insert("tableId", expectedDataItem)
    val actualDataItem = storage.selectSingle[String]("tableId", "A").right.value

    // then
    actualDataItem shouldBe expectedDataItem
  }

  it should "delete a data item" in testStorage { storage =>
    // given
    val dataItem = DataItem("A", Random.nextString(28), Seq(), Seq())

    // when
    storage.insert("tableId", dataItem)
    storage.delete("tableId", dataItem)
    val selectionResult = storage.selectSingle[String]("tableId", "A").left.value

    // then
    selectionResult shouldBe DataItemNotFound("tableId", dataItem.id)
  }

  it should "support a NoPredicate query" in testStorage { storage =>
    // given
    val dataItem = DataItem("A", Random.nextString(28), Seq(), Seq())
    storage.insert("tableId", dataItem)

    // when
    val selectionResult = storage.select[String]("tableId", NoPredicateQuery).right.value

    // then
    selectionResult shouldBe Seq(dataItem)
  }

  it should "not select from the wrong table" in testStorage { storage =>
    // given
    val expectedDataItem = DataItem("A", Random.nextString(28), Seq(), Seq())

    // when
    storage.insert("tableId", expectedDataItem)
    val actualDataItem = storage.selectSingle[String]("another-tableId", "A").left.value

    // then
    actualDataItem shouldBe DataItemNotFound("another-tableId", expectedDataItem.id)
  }

  private def testStorage(testCode: MVTableStorage => Any): Unit = {
    val tempFile: Path = Files.createTempFile("", "")
    val storage = new MVTableStorage(tempFile)
    try {
      testCode(storage)
    } finally {
      Files.delete(tempFile)
    }
  }
}
