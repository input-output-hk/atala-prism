package io.iohk.cef.data.storage.scalike.dao
import io.iohk.cef.data.storage.scalike.{DataItemTable, Schema}
import io.iohk.cef.data.{DataItem, TableId}
import io.iohk.cef.ledger.ByteStringSerializable
import scalikejdbc.{DBSession, _}

class TableStorageDao {

  def insert[I <: DataItem](tableId: TableId, dataItem: I)(
    implicit itemSerializable: ByteStringSerializable[I],
    session: DBSession): Unit = {
    val itemColumn = DataItemTable.column

    val serializedItem = itemSerializable.encode(dataItem)
    sql"""insert into ${Schema.DataTableName} (
          ${itemColumn.tableId},
          ${itemColumn.dataItem}
    ) values (
      ${tableId},
      ${serializedItem}
    )""".executeUpdate().apply()
  }


  def delete[I <: DataItem](tableId: TableId, dataItem: I)(
    implicit itemSerializable: ByteStringSerializable[I],
    session: DBSession): Unit = {
    val itemColumn = DataItemTable.column

    val serializedItem = itemSerializable.encode(dataItem)
    sql"""delete from ${Schema.DataTableName} where
          ${itemColumn.tableId} = ${tableId} and
          ${itemColumn.dataItem} = ${serializedItem}""".executeUpdate().apply()
  }
}
