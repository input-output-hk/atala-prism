package io.iohk.cef.data

sealed trait DataItemAction[I]

object DataItemAction {
  case class Insert[I](dataItem: DataItem[I]) extends DataItemAction[I]
  case class Delete[I](dataItem: DataItem[I]) extends DataItemAction[I]
}
