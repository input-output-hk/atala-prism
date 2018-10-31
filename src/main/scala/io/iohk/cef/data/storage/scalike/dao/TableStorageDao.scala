package io.iohk.cef.data.storage.scalike.dao
import akka.util.ByteString
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable}
import io.iohk.cef.data.{DataItem, Witness}
import io.iohk.cef.codecs.nio.NioEncoder
import scalikejdbc.{DBSession, _}

class TableStorageDao {
  def insert[I](dataItem: DataItem[I])(implicit session: DBSession, enc: NioEncoder[I]): Unit = {
    val itemColumn = DataItemTable.column

    val serializedItem = ByteString(enc.encode(dataItem.data))
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

  def delete[I](dataItem: DataItem[I])(implicit session: DBSession): Unit = {
    val itemColumn = DataItemTable.column
    val sigColumn = DataItemSignatureTable.column
    val owColumn = DataItemOwnerTable.column

    sql"""delete from ${DataItemSignatureTable.table} where ${sigColumn.dataItemId} = ${dataItem.id}"""
      .executeUpdate()
      .apply()
    sql"""delete from ${DataItemOwnerTable.table} where ${owColumn.dataItemId} = ${dataItem.id}"""
      .executeUpdate()
      .apply()
    sql"""delete from ${DataItemTable.table} where ${itemColumn.dataItemId} = ${dataItem.id}"""
      .executeUpdate()
      .apply()
  }

  private def insertDataItemOwners[I](dataItem: DataItem[I])(implicit session: DBSession) = {
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

  private def insertDataItemSignatures[I](dataItem: DataItem[I])(implicit session: DBSession) = {
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
