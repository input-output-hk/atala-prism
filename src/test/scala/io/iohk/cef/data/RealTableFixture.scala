package io.iohk.cef.data
import io.iohk.cef.data.storage.scalike.TableStorageImpl
import io.iohk.cef.data.storage.scalike.dao.TableStorageDao

trait RealTableFixture {
  val tableId: TableId
  val dao = new TableStorageDao

  val tableStorage = new TableStorageImpl(dao)
  val table = new Table(tableStorage)
}
