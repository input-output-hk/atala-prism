package io.iohk.cef.data

import io.iohk.cef.crypto._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.codecs.nio._

class DataItemService(table: Table) {
  def validate[I](dataItem: DataItem[I])(implicit itemSerializable: NioEncDec[I]): Boolean =
    table.validate(dataItem)

  def insert[I](tableId: TableId, dataItem: DataItem[I])(
      implicit itemSerializable: NioEncDec[I]): Either[ApplicationError, Unit] =
    table.insert(tableId, dataItem)

  def delete[I](tableId: TableId, dataItem: DataItem[I], deleteSignature: Signature)(
      implicit itemSerializable: NioEncDec[I],
      actionSerializable: NioEncDec[DataItemAction[I]]): Either[ApplicationError, Unit] =
    table.delete(tableId, dataItem, deleteSignature)
}
