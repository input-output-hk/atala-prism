package io.iohk.cef.data.storage
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.{DataItem, TableId}

trait TableStorage {

  def insert[I](tableId: TableId, dataItem: DataItem[I])(implicit itemSerializable: NioEncDec[I]): Unit

  def delete[I](tableId: TableId, dataItem: DataItem[I]): Unit
}
