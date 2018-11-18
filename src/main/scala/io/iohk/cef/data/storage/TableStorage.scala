package io.iohk.cef.data.storage
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.query.Query
import io.iohk.cef.data.{DataItem, DataItemId, TableId}
import io.iohk.cef.error.ApplicationError

trait TableStorage {

  def insert[I](tableId: TableId, dataItem: DataItem[I])(implicit itemSerializable: NioEncDec[I]): Unit

  def delete[I](tableId: TableId, dataItem: DataItem[I]): Unit

  def select[I](tableId: TableId, query: Query)(implicit nioEncDec: NioEncDec[I]): Either[ApplicationError, Seq[DataItem[I]]]

  def selectSingle[I](tableId: TableId, dataItemId: DataItemId)(
      implicit itemSerializable: NioEncDec[I]): Either[ApplicationError, DataItem[I]]

  def selectAll[I](tableId: TableId)(
      implicit
      itemSerializable: NioEncDec[I]): Either[ApplicationError, Seq[DataItem[I]]]
}
