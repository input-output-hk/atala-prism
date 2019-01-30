package io.iohk.cef.data

sealed trait LabeledItem

object LabeledItem {

  final case class Create[+T](item: T) extends LabeledItem
  final case class Delete[+T](item: T) extends LabeledItem
}
