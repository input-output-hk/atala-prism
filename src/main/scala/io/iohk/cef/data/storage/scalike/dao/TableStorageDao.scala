package io.iohk.cef.data.storage.scalike.dao
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable}
import io.iohk.cef.data.{DataItem, Witness}
import io.iohk.cef.ledger.ByteStringSerializable
import scalikejdbc.{DBSession, _}

class TableStorageDao {
  def insert[I <: DataItem](dataItem: I)(
      implicit itemSerializable: ByteStringSerializable[I],
      session: DBSession): Unit = {
    val itemColumn = DataItemTable.column

    val serializedItem = itemSerializable.encode(dataItem)
    sql"""insert into ${DataItemTable.table} (
          ${itemColumn.dataItemId},
          ${itemColumn.dataItem}
    ) values (
      ${dataItem.id},
      ${serializedItem.toArray}
    )""".executeUpdate().apply()
    insertDataItemSignatures(dataItem)
    insertDataItemOwners(dataItem)
  }

  def delete[I <: DataItem](
      dataItem: I)(implicit itemSerializable: ByteStringSerializable[I], session: DBSession): Unit = {
    val itemColumn = DataItemTable.column
    val sigColumn = DataItemSignatureTable.column
    val owColumn = DataItemOwnerTable.column

    val serializedItem = itemSerializable.encode(dataItem)
    sql"""delete from ${DataItemSignatureTable.table} where ${sigColumn.dataItemId} = ${dataItem.id}"""
      .executeUpdate()
      .apply()
    sql"""delete from ${DataItemOwnerTable.table} where ${owColumn.dataItemId} = ${dataItem.id}""".executeUpdate().apply()
    sql"""delete from ${DataItemTable.table} where ${itemColumn.dataItemId} = ${dataItem.id}"""
      .executeUpdate()
      .apply()
  }

  private def insertDataItemOwners[I <: DataItem](dataItem: I)(implicit session: DBSession) = {
    val ownerColumn = DataItemOwnerTable.column

    dataItem.owners.foreach(owner => {
      sql"""
           insert into ${DataItemOwnerTable.table} (
           ${ownerColumn.dataItemId},
           ${ownerColumn.signingPublicKey}
           ) values (
            ${dataItem.id},
            ${owner.key.toByteString.toArray}
           )
         """.executeUpdate().apply()
    })
  }

  private def insertDataItemSignatures[I <: DataItem](dataItem: I)(implicit session: DBSession) = {
    val sigColumn = DataItemSignatureTable.column
    dataItem.witnesses.foreach {
      case Witness(signature, key) =>
        sql"""insert into ${DataItemSignatureTable.table}
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
