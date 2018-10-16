package io.iohk.cef.data.storage
import io.iohk.cef.data.{DataItem, TableId}
import io.iohk.cef.ledger.ByteStringSerializable

trait TableStorage {

  def insert[I <: DataItem](tableId: TableId, dataItem: I)(implicit itemSerializable: ByteStringSerializable[I]): Unit

  def delete[I <: DataItem](tableId: TableId, dataItem: I)(implicit itemSerializable: ByteStringSerializable[I]): Unit
}
