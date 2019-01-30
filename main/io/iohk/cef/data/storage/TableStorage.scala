package io.iohk.cef.data.storage
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.query.{DataItemQuery, DataItemQueryError}
import io.iohk.cef.data.{DataItem, DataItemId, TableId}
import io.iohk.cef.error.ApplicationError

import scala.reflect.runtime.universe.TypeTag

abstract class TableStorage[I: NioCodec: TypeTag](tableId: TableId) {

  def insert(dataItem: DataItem[I]): Unit

  def delete(dataItem: DataItem[I]): Unit

  def select(query: DataItemQuery): Either[DataItemQueryError, Seq[DataItem[I]]]

  def selectSingle(dataItemId: DataItemId): Either[ApplicationError, DataItem[I]]
}
