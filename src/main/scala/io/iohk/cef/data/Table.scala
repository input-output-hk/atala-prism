package io.iohk.cef.data
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.ledger.ByteStringSerializable

class Table[I <: DataItem](tableId: TableId, tableStorage: TableStorage)(implicit itemSerializable: ByteStringSerializable[I]) {

  def insert(dataItem: I): Unit =
    tableStorage.insert(tableId, dataItem)

  def delete(dataItem: I): Unit =
    tableStorage.delete(tableId, dataItem)
}
