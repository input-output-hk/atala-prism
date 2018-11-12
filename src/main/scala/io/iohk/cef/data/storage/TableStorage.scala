package io.iohk.cef.data.storage
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.{DataItem, DataItemFactory, TableId}
import io.iohk.cef.error.ApplicationError

trait TableStorage {

  def insert[I](tableId: TableId, dataItem: DataItem[I])(implicit itemSerializable: NioEncDec[I]): Unit

  def delete[I](tableId: TableId, dataItem: DataItem[I]): Unit

  def selectAll[I](tableId: TableId)(
      implicit diFactory: DataItemFactory[I],
      itemSerializable: NioEncDec[I]): Either[ApplicationError, Seq[DataItem[I]]]
}
