package io.iohk.cef.data.storage.scalike.dao
import akka.util.ByteString
import io.iohk.cef.codecs.nio._
import io.iohk.cef.crypto._
import io.iohk.cef.data.storage.scalike.{DataItemOwnerTable, DataItemSignatureTable, DataItemTable}
import io.iohk.cef.data._
import io.iohk.cef.data.error.DataItemNotFound
import io.iohk.cef.error.ApplicationError
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
      """
      .map(rs => rs.long(it.resultName.id))
      .toOption()
      .apply()
      .getOrElse(
        throw new IllegalStateException(s"Not found: dataItemId ${dataItemId}, tableId ${tableId}")
      )
  }

  def selectAll[I](tableId: TableId)(
      implicit session: DBSession,
      serializable: NioEncDec[I]): Either[ApplicationError, Seq[DataItem[I]]] = {
    val di = DataItemTable.syntax("di")
    val dataItemRows = sql"""
        select ${di.result.*}
        from ${DataItemTable as di}
        where ${di.dataTableId} = ${tableId}
       """.map(rs => DataItemTable(di.resultName)(rs)).list().apply()
    //Inefficient for large tables
    val dataItems: Either[ApplicationError, Seq[DataItem[I]]] = transformListEither(dataItemRows.map { dir =>
      for {
        owners <- transformListEither(selectDataItemOwners(dir.id))
        witnesses <- transformListEither(selectDataItemWitnesses(dir.id))
        data <- serializable
          .decode(dir.dataItem)
          .map[Either[ApplicationError, I]](Right.apply)
          .getOrElse(Left(UnexpectedDecodingError()))
      } yield {
        DataItem(dir.dataItemId, data, witnesses, owners)
      }
    })
    dataItems
  }

  private def selectDataItemWitnesses(dataItemUniqueId: Long)(
      implicit session: DBSession): List[Either[CodecError, Witness]] = {
    val dis = DataItemSignatureTable.syntax("dis")
    sql"""
        select ${dis.result.*}
        from ${DataItemSignatureTable as dis}
        where ${dis.dataItemUniqueId} = ${dataItemUniqueId}
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

  private def selectDataItemOwners(dataItemUniqueId: Long)(
      implicit session: DBSession): List[Either[CodecError, Owner]] = {
    val dio = DataItemOwnerTable.syntax("dio")
    sql"""
        select ${dio.result.*}
        from ${DataItemOwnerTable as dio}
        where ${dio.dataItemUniqueId} = ${dataItemUniqueId}
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

  private def insertDataItemSignatures[I](dataItemUniqueId: Long, dataItem: DataItem[I])(
      implicit session: DBSession) = {
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

  def selectSingle[I](tableId: TableId, dataItemId: DataItemId)(
      implicit session: DBSession,
      serializable: NioEncDec[I]): Either[ApplicationError, DataItem[I]] =
    selectAll[I](tableId, Seq(dataItemId)).flatMap(_.headOption.toRight(new DataItemNotFound(tableId, dataItemId)))

  def selectAll[I](tableId: TableId, ids: Seq[DataItemId])(
      implicit session: DBSession,
      serializable: NioEncDec[I]): Either[CodecError, Seq[DataItem[I]]] = {
    import io.iohk.cef.utils.EitherTransforms
    val di = DataItemTable.syntax("di")
    val dataItemRows = sql"""
        select ${di.result.*}
        from ${DataItemTable as di}
        where ${di.dataItemId} in (${ids}) and ${di.dataTableId} = ${tableId}
       """.map(rs => DataItemTable(di.resultName)(rs)).list().apply()
    //Inefficient for large tables
    import EitherTransforms._
    val dataItems = dataItemRows.map(dir => {
      val uniqueId = getUniqueId(tableId, dir.dataItemId)
      val ownerEither = selectDataItemOwners(uniqueId).toEitherList
      val witnessEither = selectDataItemWitnesses(uniqueId).toEitherList
      for {
        owners <- ownerEither
        witnesses <- witnessEither
      } yield
        DataItem(
          dir.dataItemId,
          serializable
            .decode(dir.dataItem)
            .getOrElse(throw new IllegalStateException(s"Could not decode data item: ${dir}")),
          witnesses,
          owners
        )
    })
    dataItems.toEitherList
  }
}
