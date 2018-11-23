package io.iohk.cef.data.storage.mv
import java.nio.ByteBuffer
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
    table(tableId).put(dataItem.id, NioEncDec[DataItem[I]].encode(dataItem))
  }

  override def delete[I](tableId: TableId, dataItem: DataItem[I]): Unit =
    table(tableId).remove(dataItem.id)

  def decode[I: NioEncDec: TypeTag](buffers: Iterable[ByteBuffer]): Seq[DataItem[I]] = {
    val codec = NioEncDec[DataItem[I]]
    buffers.flatMap(buffer => codec.decode(buffer)).toSeq
  }

  override def select[I: NioEncDec: TypeTag](
      tableId: TableId,
      query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {

    Right(queryCata(decode(table(tableId).values().asScala), _ => Seq(), query))
  }

  override def selectSingle[I: NioEncDec: TypeTag](
      tableId: TableId,
      dataItemId: DataItemId): Either[ApplicationError, DataItem[I]] = {
    Option(table(tableId).get(dataItemId))
      .flatMap(NioEncDec[DataItem[I]].decode(_))
      .map(Right(_))
      .getOrElse(Left(DataItemNotFound(tableId, dataItemId)))
  }

  private def table(tableId: TableId): MVMap[String, ByteBuffer] =
    mvTables.table(tableId)
}
