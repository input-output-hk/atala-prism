package io.iohk.cef.data.storage.scalike
import akka.util.ByteString
import scalikejdbc._

case class DataItemTable(dataItemId: Long, tableId: String, dataItem: ByteString)

object DataItemTable extends SQLSyntaxSupport[DataItemTable] {
  override val tableName = Schema.DataTableName

  def appy(ln: ResultName[DataItemTable])(rs: WrappedResultSet): DataItemTable = {
    DataItemTable(
      rs.long(ln.dataItemId),
      rs.string(ln.tableId),
      ByteString(rs.bytes(ln.dataItem))
    )
  }
}

case class DataItemSignatureTable(dataItemId: Long, signature: ByteString, signingPublicKey: ByteString)

object DataItemSignatureTable extends SQLSyntaxSupport[DataItemSignatureTable] {
  override val tableName = Schema.DataItemSignatureTableName

  def appy(ln: ResultName[DataItemSignatureTable])(rs: WrappedResultSet): DataItemSignatureTable = {
    DataItemSignatureTable(
      rs.long(ln.dataItemId),
      ByteString(rs.bytes(ln.signature)),
      ByteString(rs.bytes(ln.signingPublicKey))
    )
  }

}

case class DataItemOwnerTable(dataItemId: Long, signingPublicKey: ByteString)

object DataItemOwnerTable extends SQLSyntaxSupport[DataItemOwnerTable] {
  override val tableName = Schema.DataItemOwnerTableName

  def appy(ln: ResultName[DataItemOwnerTable])(rs: WrappedResultSet): DataItemOwnerTable = {
    DataItemOwnerTable(
      rs.long(ln.dataItemId),
      ByteString(rs.bytes(ln.signingPublicKey))
    )
  }

}
