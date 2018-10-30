package io.iohk.cef.data.storage
import io.iohk.cef.data.{DataItem, TableId}
import io.iohk.cef.ledger.ByteStringSerializable

trait TableStorage {

  def insert[I](tableId: TableId, dataItem: DataItem[I])(implicit itemSerializable: ByteStringSerializable[I]): Unit

  def delete[I](tableId: TableId, dataItem: DataItem[I]): Unit
}
