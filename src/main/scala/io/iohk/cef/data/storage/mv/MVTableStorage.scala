package io.iohk.cef.data.storage.mv
import java.nio.file.Path

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.data.query.Query
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.data.{DataItem, DataItemId, TableId}
import io.iohk.cef.error.ApplicationError
import org.h2.mvstore.MVMap
import io.iohk.cef.data.error.DataItemNotFound
import io.iohk.cef.data.query.Query.queryCata
import io.iohk.cef.utils.mv.MVTable

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

class MVTableStorage[I: NioEncDec: TypeTag](tableId: TableId, storageFile: Path) extends TableStorage[I](tableId) {

  private val mvTable = new MVTable(tableId, storageFile, NioEncDec[DataItem[I]])

  override def insert(dataItem: DataItem[I]): Unit = {
    mvTable.update(_.put(dataItem.id, dataItem))
  }

  override def delete(dataItem: DataItem[I]): Unit =
    mvTable.update(_.remove(dataItem.id))

  override def select(query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {
    val tableValues = table.values().asScala.toSeq
    Right(queryCata(tableValues, _ => Seq[DataItem[I]](), query))
  }

  override def selectSingle(dataItemId: DataItemId): Either[ApplicationError, DataItem[I]] = {
    Option(table.get(dataItemId))
      .map(Right(_))
      .getOrElse(Left(DataItemNotFound(tableId, dataItemId)))
  }

  private def table: MVMap[String, DataItem[I]] = mvTable.table
}
