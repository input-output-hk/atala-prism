package io.iohk.cef.data.storage
import io.iohk.cef.data.DataItem
import io.iohk.cef.ledger.ByteStringSerializable

trait TableStorage {

  def insert[I](dataItem: DataItem[I])(implicit itemSerializable: ByteStringSerializable[I]): Unit

  def delete[I](dataItem: DataItem[I]): Unit
}
