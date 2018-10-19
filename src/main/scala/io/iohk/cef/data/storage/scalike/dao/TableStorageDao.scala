package io.iohk.cef.data.storage.scalike.dao
import akka.util.ByteString
import io.iohk.cef.crypto._
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable, Schema}
import io.iohk.cef.data.{DataItem, DataItemId, Owner, Witness}
import io.iohk.cef.ledger.ByteStringSerializable
import scalikejdbc.{DBSession, _}

class TableStorageDao {

  def selectAll[I <: DataItem](ids: Seq[DataItemId], dataItemCreator: (Seq[Owner], Seq[Witness], ByteString) => I)(
      implicit session: DBSession): Either[CodecError, Seq[I]] = {
    import io.iohk.cef.utils.EitherTransforms
    val di = DataItemTable.syntax("di")
    val dataItemRows = sql"""
        select ${di.result.*}
        from ${DataItemTable as di}
        where ${di.dataItemId} in ${ids}
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

  def delete[I <: DataItem](
      dataItem: I)(implicit itemSerializable: ByteStringSerializable[I], session: DBSession): Unit = {
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

    dataItem.owners.foreach(owner => {
      sql"""
           insert into ${Schema.DataItemOwnerTableName} (
           ${ownerColumn.dataItemId},
           ${ownerColumn.signingPublicKey}
           ) values (
            ${dataItem.id},
            ${owner.key.toByteString.toArray}
           )
         """.executeUpdate().apply()
    })
  }

  private def selectDataItemOwners[I <: DataItem](dataItemId: DataItemId)(implicit session: DBSession) = {
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

  private def selectDataItemWitnesses[I <: DataItem](dataItemId: DataItemId)(implicit session: DBSession) = {
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
