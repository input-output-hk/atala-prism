package io.iohk.cef.data

import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.crypto.Signature
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar._
import org.mockito.Mockito.verify

class DataItemServiceSpec extends FlatSpec {

  private val table = mock[Table]
  private val something = new DataItemService(table)
  private implicit val dataItemSerializable = mock[ByteStringSerializable[DataItem]]
  private implicit val actionSerializable = mock[ByteStringSerializable[DataItemAction[DataItem]]]

  behavior of "Something"

  it should "insert a data item" in {
    val dataItem = mock[DataItem]
    something.insert(dataItem)
    verify(table).insert(dataItem)
  }

  it should "delete a data item" in {
    val dataItem = mock[DataItem]
    val signature = mock[Signature]
    something.delete(dataItem, signature)
    verify(table).delete(dataItem, signature)
  }

  it should "validate a data item" in {
    val dataItem = mock[DataItem]
    something.validate(dataItem)
    verify(table).validate(dataItem)
  }
}
