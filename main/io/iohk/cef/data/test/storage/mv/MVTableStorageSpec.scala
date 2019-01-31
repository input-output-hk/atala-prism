package io.iohk.cef.data.storage.mv

import java.nio.file.{Files, Path}

import io.iohk.codecs.nio.auto._
import io.iohk.crypto._
import io.iohk.cef.data.error.DataItemNotFound
import io.iohk.cef.data.query.DataItemQuery._
import io.iohk.cef.data.query.Value.StringRef
import io.iohk.cef.data.query.{Field, InvalidDataItemQueryError}
import io.iohk.cef.data._
import io.iohk.cef.utils.NonEmptyList
import org.scalatest.EitherValues._
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

import scala.util.Random

class MVTableStorageSpec extends FlatSpec {

  behavior of "MVTableStorage"

  it should "insert and select a data item" in testStorage { storage =>
    // given
    val data = Random.nextString(28)
    val keys = generateSigningKeyPair()
    val owner = Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`))
    val expectedDataItem = DataItem("A", data, Seq(), NonEmptyList(owner))

    // when
    storage.insert(expectedDataItem)
    val actualDataItem = storage.selectSingle("A").right.value

    // then
    actualDataItem shouldBe expectedDataItem
  }

  it should "delete a data item" in testStorage { storage =>
    // given
    val data = Random.nextString(28)
    val keys = generateSigningKeyPair()
    val owner = Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`))
    val dataItem = DataItem("A", data, Seq(), NonEmptyList(owner))

    // when
    storage.insert(dataItem)
    storage.delete(dataItem)
    val selectionResult = storage.selectSingle("A").left.value

    // then
    selectionResult shouldBe DataItemNotFound("tableId", dataItem.id)
  }

  it should "support a NoPredicate query" in testStorage { storage =>
    // given
    val data = Random.nextString(28)
    val keys = generateSigningKeyPair()
    val owner = Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`))
    val dataItem = DataItem("A", data, Seq(), NonEmptyList(owner))
    storage.insert(dataItem)

    // when
    val selectionResult = storage.select(NoPredicateDataItemQuery).right.value

    // then
    selectionResult shouldBe Seq(dataItem)
  }

  it should "support a simple field query" in testStorage { storage =>
    // given
    val data = Random.nextString(28)
    val keys = generateSigningKeyPair()
    val owner = Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`))
    val dataItem = DataItem("A", data, Seq(), NonEmptyList(owner))
    storage.insert(dataItem)
    val query = Field(0) #== StringRef("A")

    // when
    val selectionResult = storage.select(query).right.value

    // then
    selectionResult shouldBe Seq(dataItem)
  }

  it should "support a simple field query, negative case" in testStorage { storage =>
    // given
    val data = Random.nextString(28)
    val keys = generateSigningKeyPair()
    val owner = Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`))
    val dataItem = DataItem("A", data, Seq(), NonEmptyList(owner))
    storage.insert(dataItem)
    val query = Field(0) #== StringRef("B")

    // when
    val selectionResult = storage.select(query).right.value

    // then
    selectionResult shouldBe Seq.empty
  }

  it should "return Right for an invalid query" in testStorage { storage =>
    // given
    val data = Random.nextString(28)
    val keys = generateSigningKeyPair()
    val owner = Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`))
    val dataItem = DataItem("A", data, Seq(), NonEmptyList(owner)) // mildly annoying that data is required
    storage.insert(dataItem)
    val query = Field(999) #== StringRef("A")

    // when
    val selectionResult = storage.select(query).left.value

    // then
    selectionResult shouldBe InvalidDataItemQueryError("tableId", Field(999))
  }

  it should "support an and query" in testStorage { storage =>
    // given
    val data = Random.nextString(28)
    val data2 = Random.nextString(28)
    val keys = generateSigningKeyPair()
    val owner = Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`))
    val owner2 = Owner(keys.public, sign(data2, keys.`private`))
    val dataItemA = DataItem("A", data, Seq(), NonEmptyList(owner))
    val dataItemB = DataItem("B", data2, Seq(), NonEmptyList(owner2))
    storage.insert(dataItemA)
    storage.insert(dataItemB)
    val query = (Field(0) #== StringRef("A")) and (Field(0) #== StringRef("B"))

    // when
    val selectionResult = storage.select(query).right.value

    // then
    selectionResult shouldBe Seq.empty
  }

  it should "support an or query" in testStorage { storage =>
    // given
    val data = Random.nextString(28)
    val data2 = Random.nextString(28)
    val keys = generateSigningKeyPair()
    val owner = Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`))
    val owner2 = Owner(keys.public, sign(LabeledItem.Create(data2), keys.`private`))
    val dataItemA = DataItem("A", data, Seq(), NonEmptyList(owner))
    val dataItemB = DataItem("B", data2, Seq(), NonEmptyList(owner2))
    storage.insert(dataItemA)
    storage.insert(dataItemB)
    val query = (Field(0) #== StringRef("A")) or (Field(0) #== StringRef("B"))

    // when
    val selectionResult = storage.select(query).right.value

    // then
    selectionResult shouldBe Seq(dataItemA, dataItemB)
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
