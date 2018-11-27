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
    storage.insert(expectedDataItem)
    val actualDataItem = storage.selectSingle("A").right.value

    // then
    actualDataItem shouldBe expectedDataItem
  }

  it should "delete a data item" in testStorage { storage =>
    // given
    val dataItem = DataItem("A", Random.nextString(28), Seq(), Seq())

    // when
    storage.insert(dataItem)
    storage.delete(dataItem)
    val selectionResult = storage.selectSingle("A").left.value

    // then
    selectionResult shouldBe DataItemNotFound("tableId", dataItem.id)
  }

  it should "support a NoPredicate query" in testStorage { storage =>
    // given
    val dataItem = DataItem("A", Random.nextString(28), Seq(), Seq())
    storage.insert(dataItem)

    // when
    val selectionResult = storage.select(NoPredicateQuery).right.value

    // then
    selectionResult shouldBe Seq(dataItem)
  }

  private def testStorage(testCode: MVTableStorage[String] => Any): Unit = {
    val tempFile: Path = Files.createTempFile("", "")
    val storage = new MVTableStorage[String]("tableId", tempFile)
    try {
      testCode(storage)
    } finally {
      Files.delete(tempFile)
    }
  }
}
