package io.iohk.cef.data

trait DataItemFactory[I] {
  def apply(id: DataItemId, data: I, owners: Seq[Owner], witnesses: Seq[Witness]): DataItem[I]
}
