package io.iohk.cef.data.storage
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.query.Query
import io.iohk.cef.data.{DataItem, DataItemId, TableId}
import io.iohk.cef.error.ApplicationError

import scala.reflect.runtime.universe.TypeTag

trait TableStorage {

  def insert[I: NioEncDec: TypeTag](tableId: TableId, dataItem: DataItem[I]): Unit

  def delete[I](tableId: TableId, dataItem: DataItem[I]): Unit

  def select[I: NioEncDec: TypeTag](tableId: TableId, query: Query): Either[ApplicationError, Seq[DataItem[I]]]

  def selectSingle[I: NioEncDec: TypeTag](
      tableId: TableId,
      dataItemId: DataItemId): Either[ApplicationError, DataItem[I]]
}
