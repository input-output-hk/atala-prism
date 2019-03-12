package io.iohk.cef.data

import io.iohk.cef.utils.NonEmptyList
import io.iohk.codecs.nio.NioCodec
import io.iohk.crypto._

/**
  *
  * @param data
  * @param witnesses Users/entities that witnessed this item and signed it
  * @param owners Users/entities with permission to eliminate this data item
  * @tparam T
  */
//TODO we will need to replace Seq with a simple boolean AST to better express ownership
case class DataItem[T](data: T, witnesses: Seq[Witness], owners: NonEmptyList[Owner])

object DataItem {

  type Id = String

  def id[T](dataItem: DataItem[T])(implicit codecT: NioCodec[DataItem[T]]): Id = {
    hash(dataItem).toCompactString()
  }

  object FieldIds {
    val DataItemId = 0
    val DataTableId = 1
  }
}
