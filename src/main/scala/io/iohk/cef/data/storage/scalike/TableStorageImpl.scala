package io.iohk.cef.data.storage.scalike
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.data.storage.scalike.dao.TableStorageDao
import io.iohk.cef.data.{DataItem, DataItemId, TableId}
import io.iohk.cef.error.ApplicationError
import scalikejdbc.{ConnectionPool, DB, DBSession, using}

class TableStorageImpl(tableStorageDao: TableStorageDao) extends TableStorage {

  override def insert[I](tableId: TableId, dataItem: DataItem[I])(implicit itemSerializable: NioEncDec[I]): Unit = {
    execInSession { implicit session =>
      tableStorageDao.insert(tableId, dataItem)
    }
  }

  override def delete[I](tableId: TableId, dataItem: DataItem[I]): Unit = {
    execInSession { implicit session =>
      tableStorageDao.delete(tableId, dataItem)
    }
  }

  override def selectSingle[I](tableId: TableId, dataItemId: DataItemId)(
      implicit itemSerializable: NioEncDec[I]): Either[ApplicationError, DataItem[I]] = {
    execInSession { implicit session =>
      tableStorageDao.selectSingle[I](tableId, dataItemId)
    }
  }

  override def selectAll[I](tableId: TableId)(implicit
      itemSerializable: NioEncDec[I]): Either[ApplicationError, Seq[DataItem[I]]] = {
    execInSession { implicit session =>
      tableStorageDao.selectAll(tableId)
    }
  }

  protected def execInSession[T](block: DBSession => T): T = {
    using(ConnectionPool.borrow()) { conn =>
      DB(conn).localTx(block)
    }
  }
}
