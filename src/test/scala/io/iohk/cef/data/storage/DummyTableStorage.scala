package io.iohk.cef.data.storage

import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.query.{Query, QueryError}
import io.iohk.cef.data.{DataItem, DataItemId}
import io.iohk.cef.error.ApplicationError

import scala.reflect.runtime.universe.TypeTag

class DummyTableStorage[I: NioCodec: TypeTag] extends TableStorage("tableId") {

  override def insert(dataItem: DataItem[I]): Unit = ???

  override def delete(dataItem: DataItem[I]): Unit = ???

  override def select(query: Query): Either[QueryError, Seq[DataItem[I]]] = ???

  override def selectSingle(dataItemId: DataItemId): Either[ApplicationError, DataItem[I]] = ???
}
