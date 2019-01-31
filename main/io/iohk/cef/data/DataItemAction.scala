package io.iohk.cef.data

import io.iohk.crypto.Signature

sealed trait DataItemAction[+I]

object DataItemAction {
  case class InsertAction[I](dataItem: DataItem[I]) extends DataItemAction[I]
  case class ValidateAction[I](dataItem: DataItem[I]) extends DataItemAction[I]
  case class DeleteAction[I](itemId: DataItemId, deleteSignature: Signature) extends DataItemAction[I]
}
