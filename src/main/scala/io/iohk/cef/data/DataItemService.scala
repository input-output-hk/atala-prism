package io.iohk.cef.data
import io.iohk.cef.crypto.Signature
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.ByteStringSerializable

class DataItemService(table: Table) {
  def validate[I <: DataItem](dataItem: I)(implicit itemSerializable: ByteStringSerializable[I]): Boolean =
    table.validate(dataItem)

  def insert[I <: DataItem](dataItem: I)(
      implicit itemSerializable: ByteStringSerializable[I]): Either[ApplicationError, Unit] =
    table.insert(dataItem)

  def delete[I <: DataItem](dataItem: I, deleteSignature: Signature)(
      implicit itemSerializable: ByteStringSerializable[I],
      actionSerializable: ByteStringSerializable[DataItemAction[I]]): Either[ApplicationError, Unit] =
    table.delete(dataItem, deleteSignature)
}
