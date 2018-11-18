package io.iohk.cef.data.storage.scalike
import java.nio.ByteBuffer

import akka.util.ByteString
import io.iohk.cef.data.{DataItemId, TableId}
import io.iohk.cef.db.scalike.SqlFieldGetter
import scalikejdbc._

case class DataItemTable(id: Long, dataTableId: TableId, dataItemId: DataItemId, dataItem: ByteBuffer)

object DataItemTable extends SQLSyntaxSupport[DataItemTable] with SqlFieldGetter[DataItemTable] {
  override val tableName = Schema.DataTableName

  def apply(ln: ResultName[DataItemTable])(rs: WrappedResultSet): DataItemTable = {
    DataItemTable(
      rs.long(ln.id),
      rs.string(ln.dataTableId),
      rs.string(ln.dataItemId),
      ByteBuffer.wrap(rs.bytes(ln.dataItem))
    )
  }

  override def getField(index: Int, di: QuerySQLSyntaxProvider[SQLSyntaxSupport[DataItemTable], DataItemTable]): SQLSyntax = index match {
    case 0 => di.dataItemId
    case 1 => di.dataItem
  }
}

case class DataItemSignatureTable(dataItemUniqueId: Long, signature: ByteString, signingPublicKey: ByteString)

object DataItemSignatureTable extends SQLSyntaxSupport[DataItemSignatureTable] {
  override val tableName = Schema.DataItemSignatureTableName

  def apply(ln: ResultName[DataItemSignatureTable])(rs: WrappedResultSet): DataItemSignatureTable = {
    DataItemSignatureTable(
      rs.long(ln.dataItemUniqueId),
      ByteString(rs.bytes(ln.signature)),
      ByteString(rs.bytes(ln.signingPublicKey))
    )
  }

}

case class DataItemOwnerTable(dataItemUniqueId: Long, signingPublicKey: ByteString)

object DataItemOwnerTable extends SQLSyntaxSupport[DataItemOwnerTable] {
  override val tableName = Schema.DataItemOwnerTableName

  def apply(ln: ResultName[DataItemOwnerTable])(rs: WrappedResultSet): DataItemOwnerTable = {
    DataItemOwnerTable(
      rs.long(ln.dataItemUniqueId),
      ByteString(rs.bytes(ln.signingPublicKey))
    )
  }

}
