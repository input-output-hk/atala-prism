package io.iohk.cef.data

sealed trait DataItemAction[I <: DataItem]

object DataItemAction {
  case class Insert[I <: DataItem](dataItem: I) extends DataItemAction[I]
  case class Delete[I <: DataItem](dataItem: I) extends DataItemAction[I]
}
