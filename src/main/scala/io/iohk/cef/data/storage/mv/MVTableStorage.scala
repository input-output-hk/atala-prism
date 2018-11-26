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
import io.iohk.cef.utils.mv.MVTables

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

class MVTableStorage(storageFile: Path) extends TableStorage {

  private val mvTables = new MVTables(storageFile)

  override def insert[I: NioEncDec: TypeTag](tableId: TableId, dataItem: DataItem[I]): Unit = {
    table[DataItem[I]](tableId).put(dataItem.id, dataItem)
  }

  override def delete[I: NioEncDec: TypeTag](tableId: TableId, dataItem: DataItem[I]): Unit =
    table[DataItem[I]](tableId).remove(dataItem.id)

  override def select[I: NioEncDec: TypeTag](
      tableId: TableId,
      query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {

    val tableValues = table[DataItem[I]](tableId).values().asScala.toSeq
    Right(queryCata(tableValues, _ => Seq[DataItem[I]](), query))
  }

  override def selectSingle[I: NioEncDec: TypeTag](
      tableId: TableId,
      dataItemId: DataItemId): Either[ApplicationError, DataItem[I]] = {
    Option(table[DataItem[I]](tableId).get(dataItemId))
      .map(Right(_))
      .getOrElse(Left(DataItemNotFound(tableId, dataItemId)))
  }

  private def table[I: NioEncDec: TypeTag](tableId: TableId): MVMap[String, I] =
    mvTables.table(tableId, NioEncDec[I])
}
