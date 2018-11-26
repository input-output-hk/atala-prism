package io.iohk.cef.data.storage.scalike
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.query.Query
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.data.storage.scalike.dao.TableStorageDao
import io.iohk.cef.data.{DataItem, DataItemId, TableId}
import io.iohk.cef.error.ApplicationError
import scalikejdbc._
import scala.reflect.runtime.universe._

class TableStorageImpl(tableStorageDao: TableStorageDao) extends TableStorage {

  override def insert[I: NioEncDec: TypeTag](tableId: TableId, dataItem: DataItem[I]): Unit = {
    execInSession { implicit session =>
      tableStorageDao.insert(tableId, dataItem)
    }
  }

  override def delete[I: NioEncDec: TypeTag](tableId: TableId, dataItem: DataItem[I]): Unit = {
    execInSession { implicit session =>
      tableStorageDao.delete(tableId, dataItem)
    }
  }

  override def select[I: NioEncDec: TypeTag](
      tableId: TableId,
      query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {
    execInSession { implicit session =>
      tableStorageDao.selectWithQuery(tableId, query)
    }
  }

  override def selectSingle[I: NioEncDec: TypeTag](
      tableId: TableId,
      dataItemId: DataItemId): Either[ApplicationError, DataItem[I]] = {
    execInSession { implicit session =>
      tableStorageDao.selectSingle[I](tableId, dataItemId)
    }
  }

  protected def execInSession[T](block: DBSession => T): T = {
    using(ConnectionPool.borrow()) { conn =>
      DB(conn).localTx(block)
    }
  }
}
