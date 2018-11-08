package io.iohk.cef.data

import io.iohk.cef.crypto.Signature

sealed trait DataItemAction[I] {
  def dataItem: DataItem[I]
}

object DataItemAction {
  case class Insert[I](dataItem: DataItem[I]) extends DataItemAction[I]
  case class Delete[I](dataItem: DataItem[I], deleteSignature: Signature) extends DataItemAction[I]
}
