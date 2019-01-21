package io.iohk.cef.data

import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data.error.InvalidSignaturesError
import io.iohk.cef.data.storage.DummyTableStorage
import io.iohk.cef.utils.NonEmptyList
import org.scalatest.EitherValues._
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec

class TableSpec extends WordSpec {

  val tableId = "table"
  val storage = new DummyTableStorage[String] {
    override def insert(dataItem: DataItem[String]): Unit = ()
  }

  implicit val validator: CanValidate[DataItem[String]] = _ => Right(())

  val table = new Table[String](tableId, storage)
  val keys = generateSigningKeyPair()

  "insert" should {
    "succeed with a valid item with the correct signatures" in {
      val data = "data"
      val witnesses = List(Witness(keys.public, sign(data, keys.`private`)))
      val owners = NonEmptyList(Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`)))
      val item = DataItem("id", data, witnesses, owners)
      val result = table.insert(item)

      result.right.value must be(())
    }

    "reject an item when the witnesses signature can't be verified" in {
      val data = "data"
      val invalidSignature = sign(data * 2, keys.`private`)
      val witnesses = List(Witness(keys.public, invalidSignature))
      val owners = NonEmptyList(Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`)))
      val item = DataItem("id", data, witnesses, owners)
      val result = table.insert(item)

      result.left.value must be(InvalidSignaturesError(item, List(invalidSignature)))
    }

    "reject an item when the owner signature can't be verified" in {
      val data = "data"
      val invalidSignature = sign(data, keys.`private`)
      val witnesses = List(Witness(keys.public, sign(data, keys.`private`)))
      val owners = NonEmptyList(Owner(keys.public, invalidSignature))
      val item = DataItem("id", data, witnesses, owners)
      val result = table.insert(item)

      result.left.value must be(InvalidSignaturesError(item, List(invalidSignature)))
    }
  }
}
