package io.iohk.cef.data.storage
import io.iohk.cef.data.DataItem
import io.iohk.cef.codecs.nio.NioEncoder

trait TableStorage {

  def insert[I: NioEncoder](dataItem: DataItem[I]): Unit

  def delete[I](dataItem: DataItem[I]): Unit
}
