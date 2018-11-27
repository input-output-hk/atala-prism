package io.iohk.cef.data

import io.iohk.cef.crypto.Signature

sealed trait DataItemAction[I]

object DataItemAction {
  def insert[I](dataItem: DataItem[I]): DataItemAction[I] = InsertAction(dataItem)
  def delete[I](itemId: DataItemId, deleteSignature: Signature): DataItemAction[I] =
    DeleteAction(itemId, deleteSignature)

  case class InsertAction[I](dataItem: DataItem[I]) extends DataItemAction[I]
  case class DeleteAction[I](itemId: DataItemId, deleteSignature: Signature) extends DataItemAction[I]
}
