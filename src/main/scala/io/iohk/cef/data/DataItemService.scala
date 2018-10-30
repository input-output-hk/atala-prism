package io.iohk.cef.data
import io.iohk.cef.crypto.Signature
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.ByteStringSerializable

class DataItemService(table: Table) {
  def validate[I](dataItem: DataItem[I])(
      implicit itemSerializable: ByteStringSerializable[I],
      canValidate: CanValidate[DataItem[I]]): Boolean =
    table.validate(dataItem)

  def insert[I](dataItem: DataItem[I])(
      implicit itemSerializable: ByteStringSerializable[I],
      canValidate: CanValidate[DataItem[I]]): Either[ApplicationError, Unit] =
    table.insert(dataItem)

  def delete[I](dataItem: DataItem[I], deleteSignature: Signature)(
      implicit itemSerializable: ByteStringSerializable[I],
      actionSerializable: ByteStringSerializable[DataItemAction[I]],
      canValidate: CanValidate[DataItem[I]]): Either[ApplicationError, Unit] =
    table.delete(dataItem, deleteSignature)
}
