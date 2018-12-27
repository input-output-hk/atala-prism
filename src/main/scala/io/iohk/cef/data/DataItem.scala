package io.iohk.cef.data

import org.scalactic.Every

/**
  *
  * @param id
  * @param data
  * @param witnesses Users/entities that witnessed this item and signed it
  * @param owners Users/entities with permission to eliminate this data item
  * @tparam T
  */
//TODO we will need to replace Seq with a simple boolean AST to better express ownership
case class DataItem[T](id: String, data: T, witnesses: Seq[Witness], owners: Every[Owner])

object DataItem {
  object FieldIds {
    val DataItemId = 0
    val DataTableId = 1
  }
}
