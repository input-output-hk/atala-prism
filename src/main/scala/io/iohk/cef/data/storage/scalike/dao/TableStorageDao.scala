package io.iohk.cef.data.storage.scalike.dao
import akka.util.ByteString
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable}
import io.iohk.cef.data.{DataItem, DataItemId, TableId, Witness}
import scalikejdbc.{DBSession, _}

class TableStorageDao {
  def insert[I](tableId: TableId, dataItem: DataItem[I])(implicit session: DBSession, enc: NioEncoder[I]): Unit = {
    val itemColumn = DataItemTable.column

    val serializedItem = ByteString(enc.encode(dataItem.data))
    sql"""insert into ${DataItemTable.table} (
          ${itemColumn.dataTableId},
          ${itemColumn.dataItemId},
          ${itemColumn.dataItem}
    ) values (
      ${tableId},
      ${dataItem.id},
      ${serializedItem.toArray}
    )""".executeUpdate().apply()
    val uniqueId = getUniqueId(tableId, dataItem.id)
    insertDataItemSignatures(uniqueId, dataItem)
    insertDataItemOwners(uniqueId, dataItem)
  }

  def delete[I](tableId: TableId, dataItem: DataItem[I])(implicit session: DBSession): Unit = {
    val itemColumn = DataItemTable.column
    val sigColumn = DataItemSignatureTable.column
    val owColumn = DataItemOwnerTable.column

    val uniqueId = getUniqueId(tableId, dataItem.id)

    sql"""delete from ${DataItemSignatureTable.table} where ${sigColumn.dataItemUniqueId} = ${uniqueId}"""
      .executeUpdate()
      .apply()
    sql"""delete from ${DataItemOwnerTable.table} where ${owColumn.dataItemUniqueId} = ${uniqueId}"""
      .executeUpdate()
      .apply()
    sql"""delete from ${DataItemTable.table} where ${itemColumn.id} = ${uniqueId}"""
      .executeUpdate()
      .apply()
  }

  private def getUniqueId(tableId: TableId, dataItemId: DataItemId)(implicit DBSession: DBSession): Long = {
    val it = DataItemTable.syntax("it")

    sql"""
      select ${it.result.id} from ${DataItemTable as it}
       where ${it.dataTableId} = ${tableId} and ${it.dataItemId} = ${dataItemId}
      """.map(rs => rs.long(it.resultName.id)).toOption().apply().getOrElse(
      throw new IllegalStateException(s"Not found: dataItemId ${dataItemId}, tableId ${tableId}")
    )
  }

  private def insertDataItemOwners[I](dataItemUniqueId: Long, dataItem: DataItem[I])(implicit session: DBSession) = {
    val ownerColumn = DataItemOwnerTable.column

    dataItem.owners.foreach(owner => {
      sql"""
           insert into ${DataItemOwnerTable.table} (
           ${ownerColumn.dataItemUniqueId},
           ${ownerColumn.signingPublicKey}
           ) values (
            ${dataItemUniqueId},
            ${owner.key.toByteString.toArray}
           )
         """.executeUpdate().apply()
    })
  }

  private def insertDataItemSignatures[I](dataItemUniqueId: Long, dataItem: DataItem[I])(implicit session: DBSession) = {
    val sigColumn = DataItemSignatureTable.column
    dataItem.witnesses.foreach {
      case Witness(signature, key) =>
        sql"""insert into ${DataItemSignatureTable.table}
              (${sigColumn.dataItemUniqueId},
              ${sigColumn.signature},
              ${sigColumn.signingPublicKey})
              values (
              ${dataItemUniqueId},
              ${signature.toByteString.toArray},
              ${key.toByteString.toArray}
              )
           """.executeUpdate().apply()
    }
  }
}
