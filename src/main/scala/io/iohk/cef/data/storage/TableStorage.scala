package io.iohk.cef.data.storage
import io.iohk.cef.data.DataItem
import io.iohk.cef.ledger.ByteStringSerializable

trait TableStorage {

  def insert[I <: DataItem](dataItem: I)(implicit itemSerializable: ByteStringSerializable[I]): Unit

  def delete[I <: DataItem](dataItem: I)(implicit itemSerializable: ByteStringSerializable[I]): Unit
}
