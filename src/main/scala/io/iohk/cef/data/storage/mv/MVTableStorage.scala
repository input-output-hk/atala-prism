package io.iohk.cef.data.storage.mv
import java.nio.ByteBuffer
import java.nio.file.Path

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.data.query.Query
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.data.{DataItem, DataItemId, TableId}
import io.iohk.cef.error.ApplicationError
import org.h2.mvstore.{MVMap, MVStore}
import java.util.concurrent.ConcurrentHashMap

import io.iohk.cef.data.error.DataItemNotFound

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

class MVTableStorage(storageFile: Path) extends TableStorage {

  private val storage: MVStore = new MVStore.Builder().fileName(storageFile.toAbsolutePath.toString).open()

  private val tables = new ConcurrentHashMap[TableId, MVMap[String, ByteBuffer]]().asScala

  override def insert[I: NioEncDec: TypeTag](tableId: TableId, dataItem: DataItem[I]): Unit = {
    table(tableId).put(dataItem.id, NioEncDec[DataItem[I]].encode(dataItem))
  }

  override def delete[I](tableId: TableId, dataItem: DataItem[I]): Unit = ???

  override def select[I: NioEncDec: TypeTag](
      tableId: TableId,
      query: Query): Either[ApplicationError, Seq[DataItem[I]]] = ???

  override def selectSingle[I: NioEncDec: TypeTag](
      tableId: TableId,
      dataItemId: DataItemId): Either[ApplicationError, DataItem[I]] = {
    Option(table(tableId).get(dataItemId))
      .flatMap(NioEncDec[DataItem[I]].decode(_))
      .map(Right(_))
      .getOrElse(Left(new DataItemNotFound(tableId, dataItemId)))
  }

  private def table(tableId: TableId): MVMap[String, ByteBuffer] =
    tables.getOrElseUpdate(tableId, storage.openMap(tableId))
}
