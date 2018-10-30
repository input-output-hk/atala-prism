package io.iohk.cef.data

import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.crypto.Signature
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar._
import org.mockito.Mockito.verify

class DataItemServiceSpec extends FlatSpec {

  private val table = mock[Table]
  private val something = new DataItemService(table)
  private implicit val dataItemSerializable = mock[ByteStringSerializable[String]]
  private implicit val actionSerializable = mock[ByteStringSerializable[DataItemAction[String]]]
  val tableId: TableId = "Table"

  behavior of "DataItemService"

  it should "insert a data item" in {
    val dataItem: DataItem[String] = mock[DataItem[String]]
    something.insert(tableId, dataItem)
    verify(table).insert(tableId, dataItem)
  }

  it should "delete a data item" in {
    val dataItem: DataItem[String] = mock[DataItem[String]]
    val signature = mock[Signature]
    something.delete(tableId, dataItem, signature)
    verify(table).delete(tableId, dataItem, signature)
  }

  it should "validate a data item" in {
    val dataItem: DataItem[String] = mock[DataItem[String]]
    something.validate(dataItem)
    verify(table).validate(dataItem)
  }
}
