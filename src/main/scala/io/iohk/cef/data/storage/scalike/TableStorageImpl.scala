package io.iohk.cef.data.storage.scalike
import io.iohk.cef.data.DataItem
import io.iohk.cef.data.storage.TableStorage
import io.iohk.cef.data.storage.scalike.dao.TableStorageDao
import io.iohk.cef.codecs.nio.NioEncoder
import scalikejdbc.{ConnectionPool, DB, DBSession, using}

class TableStorageImpl(tableStorageDao: TableStorageDao) extends TableStorage {

  override def insert[I: NioEncoder](dataItem: DataItem[I]): Unit = {
    execInSession { implicit session =>
      tableStorageDao.insert(dataItem)
    }
  }

  override def delete[I](dataItem: DataItem[I]): Unit = {
    execInSession { implicit session =>
      tableStorageDao.delete(dataItem)
    }
  }

  protected def execInSession[T](block: DBSession => T): T = {
    using(ConnectionPool.borrow()) { conn =>
      DB(conn).localTx(block)
    }
  }
}
