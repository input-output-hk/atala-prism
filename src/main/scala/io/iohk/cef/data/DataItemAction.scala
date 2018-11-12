package io.iohk.cef.data

import io.iohk.cef.crypto.Signature

sealed trait DataItemAction[I]

object DataItemAction {
  case class Insert[I](dataItem: DataItem[I]) extends DataItemAction[I]
  case class Delete[I](itemId: DataItemId, deleteSignature: Signature) extends DataItemAction[I]
}
