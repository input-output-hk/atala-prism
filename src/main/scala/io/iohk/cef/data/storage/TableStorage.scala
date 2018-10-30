package io.iohk.cef.data.storage
import io.iohk.cef.TableId
import io.iohk.cef.data.{DataItem, DataItemFactory}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.ByteStringSerializable

trait TableStorage {

  def insert[I](tableId: TableId, dataItem: DataItem[I])(implicit itemSerializable: ByteStringSerializable[I]): Unit

  def delete[I](tableId: TableId, dataItem: DataItem[I]): Unit

  def selectAll[I](tableId: TableId)(
      implicit diFactory: DataItemFactory[I],
      itemSerializable: ByteStringSerializable[I]): Either[ApplicationError, Seq[DataItem[I]]]
}
