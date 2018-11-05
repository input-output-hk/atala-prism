package io.iohk.cef.data.storage.scalike
import io.iohk.cef.data.{DataItem, TableId}
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.data.storage.scalike.dao.TableStorageDao
import scalikejdbc.{ConnectionPool, DB, DBSession, using}
import io.iohk.cef.codecs.nio.NioEncDec

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

  protected def execInSession[T](block: DBSession => T): T = {
    using(ConnectionPool.borrow()) { conn =>
      DB(conn).localTx(block)
    }
  }
}
