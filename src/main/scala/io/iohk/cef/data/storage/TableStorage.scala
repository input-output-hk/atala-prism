package io.iohk.cef.data.storage
import io.iohk.cef.data.DataItem
import io.iohk.cef.codecs.nio.NioEncDec

trait TableStorage {

  def insert[I](dataItem: DataItem[I])(implicit itemSerializable: NioEncDec[I]): Unit

  def delete(dataItemId: String): Unit
}
