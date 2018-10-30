package io.iohk.cef.data
import io.iohk.cef.crypto.Signature
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.ByteStringSerializable

class DataItemService(table: Table) {
  def validate[I](dataItem: DataItem[I])(implicit itemSerializable: ByteStringSerializable[I]): Boolean =
    table.validate(dataItem)

  def insert[I](tableId: TableId, dataItem: DataItem[I])(
      implicit itemSerializable: ByteStringSerializable[I]): Either[ApplicationError, Unit] =
    table.insert(tableId, dataItem)

  def delete[I](tableId: TableId, dataItem: DataItem[I], deleteSignature: Signature)(
      implicit itemSerializable: ByteStringSerializable[I],
      actionSerializable: ByteStringSerializable[DataItemAction[I]]): Either[ApplicationError, Unit] =
    table.delete(tableId, dataItem, deleteSignature)
}
