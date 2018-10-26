package io.iohk.cef.data.storage
import io.iohk.cef.TableId
import io.iohk.cef.data.DataItem
import io.iohk.cef.ledger.ByteStringSerializable

trait TableStorage {

  def insert[I](dataItem: DataItem[I])(implicit itemSerializable: ByteStringSerializable[I]): Unit

  def delete[I](dataItem: DataItem[I]): Unit

  def selectAll[I](tableId: TableId)(implicit itemSerializable: ByteStringSerializable[I]): Seq[DataItem[I]]
}
