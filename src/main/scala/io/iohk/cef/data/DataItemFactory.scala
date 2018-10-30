package io.iohk.cef.data

//Not needed if DataItem is a case class
trait DataItemFactory[I] {
  def apply(id: DataItemId, data: I, owners: Seq[Owner], witnesses: Seq[Witness]): DataItem[I]
}
