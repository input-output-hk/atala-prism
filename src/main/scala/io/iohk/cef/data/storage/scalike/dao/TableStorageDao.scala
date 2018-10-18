package io.iohk.cef.data.storage.scalike.dao
import akka.util.ByteString
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable, Schema}
import io.iohk.cef.data.{DataItem, Owner, TableId, Witness}
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.storage.scalike.DataLayerException
import scalikejdbc.{DBSession, _}

class TableStorageDao {

  def selectAll[I <: DataItem](tableId: TableId, dataItemCreator: (Seq[Owner], Seq[Witness], ByteString) => I)(
      implicit session: DBSession): Either[CodecError, Seq[I]] = {
    import io.iohk.cef.utils.EitherTransforms
    val di = DataItemTable.syntax("di")
    val dataItemRows = sql"""
        select ${di.result.*}
        from ${DataItemTable as di}
        where ${di.tableId} = ${tableId}
       """.map(rs => DataItemTable(di.resultName)(rs)).list().apply()
    //Inefficient for large tables
    import EitherTransforms._
    val dataItems = dataItemRows.map(dir => {
      val ownerEither = selectDataItemOwners(dir.dataItemId).toEitherList
      val witnessEither = selectDataItemWitnesses(dir.dataItemId).toEitherList
      for {
        owners <- ownerEither
        witnesses <- witnessEither
      } yield dataItemCreator(owners, witnesses, dir.dataItem)
    })
    dataItems.toEitherList
  }

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

    dataItem.owners.foreach(owner => {
      sql"""
           insert into ${Schema.DataItemOwnerTableName} (
           ${ownerColumn.dataItemId},
           ${ownerColumn.signingPublicKey}
           ) values (
            ${dataItemId},
            ${owner.publicKey.toByteString.toArray}
           )
         """.executeUpdate().apply()
    })
  }

  private def selectDataItemOwners[I <: DataItem](dataItemId: Long)(implicit session: DBSession) = {
    val dio = DataItemOwnerTable.syntax("dio")
    sql"""
        select ${dio.result.*}
        from ${DataItemOwnerTable as dio}
        where ${dio.dataItemId} = ${dataItemId}
       """
      .map(rs => SigningPublicKey.decodeFrom(DataItemOwnerTable(dio.resultName)(rs).signingPublicKey).map(Owner))
      .list()
      .apply()
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
      case Witness(signature, key) =>
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

  private def selectDataItemWitnesses[I <: DataItem](dataItemId: Long)(implicit session: DBSession) = {
    val dis = DataItemSignatureTable.syntax("dis")
    sql"""
        select ${dis.result.*}
        from ${DataItemSignatureTable as dis}
        where ${dis.dataItemId} = ${dataItemId}
       """
      .map(rs => {
        val row = DataItemSignatureTable(dis.resultName)(rs)
        for {
          key <- SigningPublicKey.decodeFrom(row.signingPublicKey)
          sig <- Signature.decodeFrom(row.signature)
        } yield Witness(key, sig)
      })
      .list()
      .apply()
  }
}
