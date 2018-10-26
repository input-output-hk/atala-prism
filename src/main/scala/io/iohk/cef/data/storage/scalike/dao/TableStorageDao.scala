package io.iohk.cef.data.storage.scalike.dao
import io.iohk.cef.TableId
import io.iohk.cef.crypto._
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable}
import io.iohk.cef.data._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.ByteStringSerializable
import scalikejdbc.{DBSession, _}

class TableStorageDao {
  def insert[I](
      dataItem: DataItem[I])(implicit itemSerializable: ByteStringSerializable[I], session: DBSession): Unit = {
    val itemColumn = DataItemTable.column

    val serializedItem = itemSerializable.encode(dataItem.data)
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

  def selectAll[I](tableId: TableId)(
      implicit diFactory: DataItemFactory[I],
      session: DBSession,
      serializable: ByteStringSerializable[I]): Either[ApplicationError, Seq[DataItem[I]]] = {
    val di = DataItemTable.syntax("di")
    val dataItemRows = sql"""
        select ${di.result.*}
        from ${DataItemTable as di}
        where ${di.tableId} = ${tableId}
       """.map(rs => DataItemTable(di.resultName)(rs)).list().apply()
    //Inefficient for large tables
    val dataItems: Either[ApplicationError, Seq[DataItem[I]]] = transformListEither(dataItemRows.map { dir =>
      for {
        owners <- transformListEither(selectDataItemOwners(dir.dataItemId))
        witnesses <- transformListEither(selectDataItemWitnesses(dir.dataItemId))
        data <- serializable
          .decode(dir.dataItem)
          .map[Either[ApplicationError, I]](Right.apply)
          .getOrElse(Left(UnexpectedDecodingError()))
      } yield {
        diFactory(dir.dataItemId, data, owners, witnesses)
      }
    })
    dataItems
  }

  private def selectDataItemWitnesses(dataItemId: DataItemId)(
      implicit session: DBSession): List[Either[CodecError, Witness]] = {
    val dis = DataItemSignatureTable.syntax("dis")
    sql"""
        select ${dis.result.*}
        from ${DataItemSignatureTable as dis}
        where ${dis.dataItemId} = ${dataItemId}
       """
      .map { rs =>
        val row = DataItemSignatureTable(dis.resultName)(rs)
        for {
          key <- SigningPublicKey.decodeFrom(row.signingPublicKey)
          sig <- Signature.decodeFrom(row.signature)
        } yield Witness(key, sig)
      }
      .list()
      .apply()
  }

  private def selectDataItemOwners(dataItemId: DataItemId)(
      implicit session: DBSession): List[Either[CodecError, Owner]] = {
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

  private def transformListEither[A, B](list: Seq[Either[A, B]]): Either[A, Seq[B]] = {
    list.foldLeft[Either[A, Seq[B]]](Right(Seq()))((state, curr) => {
      for {
        s <- state
        c <- curr
      } yield c +: s
    })
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
