package io.iohk.cef.data.storage.scalike.dao
import akka.util.ByteString
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable, Schema}
import io.iohk.cef.data.{DataItem, TableId}
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.ledger.storage.scalike.DataLayerException
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
      ${serializedItem.toArray}
    )""".executeUpdate().apply()
    val dataItemId = getDataItemId(tableId, serializedItem).getOrElse(
      throw new DataLayerException(s"Could not insert data item ${dataItem} in table ${tableId}")
    )
    insertDataItemSignatures(dataItem, dataItemId)
    insertDataItemOwners(dataItem, dataItemId)
  }

  def delete[I <: DataItem](tableId: TableId, dataItem: I)(
      implicit itemSerializable: ByteStringSerializable[I],
      session: DBSession): Unit = {
    val itemColumn = DataItemTable.column
    val sigColumn = DataItemSignatureTable.column
    val owColumn = DataItemOwnerTable.column

    val serializedItem = itemSerializable.encode(dataItem)
    val dataItemId = getDataItemId(tableId, serializedItem).getOrElse(
      throw new DataLayerException(s"Could not insert data item ${dataItem} in table ${tableId}")
    )
    sql"""delete from ${Schema.DataItemSignatureTableName} where ${sigColumn.dataItemId} = ${dataItemId}"""
      .executeUpdate()
      .apply()
    sql"""delete from ${Schema.DataTableName} where ${owColumn.dataItemId} = ${dataItemId}""".executeUpdate().apply()
    sql"""delete from ${Schema.DataItemOwnerTableName} where ${itemColumn.dataItemId} = ${dataItemId}"""
      .executeUpdate()
      .apply()
  }

  private def insertDataItemOwners[I <: DataItem](dataItem: I, dataItemId: Long)(implicit session: DBSession) = {
    val ownerColumn = DataItemOwnerTable.column

    dataItem.owners.foreach(ownerKey => {
      sql"""
           insert into ${Schema.DataItemOwnerTableName} (
           ${ownerColumn.dataItemId},
           ${ownerColumn.signingPublicKey}
           ) values (
            ${dataItemId},
            ${ownerKey.toByteString.toArray}
           )
         """.executeUpdate().apply()
    })
  }

  private def getDataItemId(tableId: TableId, dataItem: ByteString)(implicit session: DBSession): Option[Long] = {
    val dt = DataItemTable.syntax("dt")

    sql"""
         select ${dt.result.dataItemId}
         from ${DataItemTable as dt}
         where ${dt.tableId} = ${tableId} and ${dt.dataItem} = ${dataItem.toArray}
       """.map(rs => rs.long(dt.resultName.dataItemId)).toOption().apply()
  }

  private def insertDataItemSignatures[I <: DataItem](dataItem: I, dataItemId: Long)(implicit session: DBSession) = {
    val sigColumn = DataItemSignatureTable.column
    dataItem.witnesses.foreach {
      case (signature, key) =>
        sql"""insert into ${Schema.DataItemSignatureTableName}
              (${sigColumn.dataItemId},
              ${sigColumn.signature},
              ${sigColumn.signingPublicKey})
              values (
              ${dataItemId},
              ${signature.toByteString.toArray},
              ${key.toByteString.toArray}
              )
           """.executeUpdate().apply()
    }
  }
}
