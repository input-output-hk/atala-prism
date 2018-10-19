package io.iohk.cef.data.storage.scalike.dao
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable, Schema}
import io.iohk.cef.data.{DataItem, Witness}
import io.iohk.cef.ledger.ByteStringSerializable
import scalikejdbc.{DBSession, _}

class TableStorageDao {

  def insert[I <: DataItem](dataItem: I)(
      implicit itemSerializable: ByteStringSerializable[I],
      session: DBSession): Unit = {
    val itemColumn = DataItemTable.column

    val serializedItem = itemSerializable.encode(dataItem)
    sql"""insert into ${Schema.DataTableName} (
          ${itemColumn.dataItemId},
          ${itemColumn.dataItem}
    ) values (
      ${dataItem.id},
      ${serializedItem.toArray}
    )""".executeUpdate().apply()
    insertDataItemSignatures(dataItem)
    insertDataItemOwners(dataItem)
  }

  def delete[I <: DataItem](dataItem: I)(
      implicit itemSerializable: ByteStringSerializable[I],
      session: DBSession): Unit = {
    val itemColumn = DataItemTable.column
    val sigColumn = DataItemSignatureTable.column
    val owColumn = DataItemOwnerTable.column

    val serializedItem = itemSerializable.encode(dataItem)
    sql"""delete from ${Schema.DataItemSignatureTableName} where ${sigColumn.dataItemId} = ${dataItem.id}"""
      .executeUpdate()
      .apply()
    sql"""delete from ${Schema.DataTableName} where ${owColumn.dataItemId} = ${dataItem.id}""".executeUpdate().apply()
    sql"""delete from ${Schema.DataItemOwnerTableName} where ${itemColumn.dataItemId} = ${dataItem.id}"""
      .executeUpdate()
      .apply()
  }

  private def insertDataItemOwners[I <: DataItem](dataItem: I)(implicit session: DBSession) = {
    val ownerColumn = DataItemOwnerTable.column

    dataItem.owners.foreach(ownerKey => {
      sql"""
           insert into ${Schema.DataItemOwnerTableName} (
           ${ownerColumn.dataItemId},
           ${ownerColumn.signingPublicKey}
           ) values (
            ${dataItem.id},
            ${ownerKey.key.toByteString.toArray}
           )
         """.executeUpdate().apply()
    })
  }

  private def insertDataItemSignatures[I <: DataItem](dataItem: I)(implicit session: DBSession) = {
    val sigColumn = DataItemSignatureTable.column
    dataItem.witnesses.foreach {
      case Witness(signature, key) =>
        sql"""insert into ${Schema.DataItemSignatureTableName}
              (${sigColumn.dataItemId},
              ${sigColumn.signature},
              ${sigColumn.signingPublicKey})
              values (
              ${dataItem.id},
              ${signature.toByteString.toArray},
              ${key.toByteString.toArray}
              )
           """.executeUpdate().apply()
    }
  }
}
