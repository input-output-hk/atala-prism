package io.iohk.cef.data.storage.scalike
import akka.util.ByteString
import scalikejdbc._

case class DataItemTable(dataItemId: String, dataItem: ByteString)

object DataItemTable extends SQLSyntaxSupport[DataItemTable] {
  override val tableName = Schema.DataTableName

  def apply(ln: ResultName[DataItemTable])(rs: WrappedResultSet): DataItemTable = {
    DataItemTable(
      rs.string(ln.dataItemId),
      ByteString(rs.bytes(ln.dataItem))
    )
  }
}

case class DataItemSignatureTable(dataItemId: String, signature: ByteString, signingPublicKey: ByteString)

object DataItemSignatureTable extends SQLSyntaxSupport[DataItemSignatureTable] {
  override val tableName = Schema.DataItemSignatureTableName

  def apply(ln: ResultName[DataItemSignatureTable])(rs: WrappedResultSet): DataItemSignatureTable = {
    DataItemSignatureTable(
      rs.string(ln.dataItemId),
      ByteString(rs.bytes(ln.signature)),
      ByteString(rs.bytes(ln.signingPublicKey))
    )
  }

}

case class DataItemOwnerTable(dataItemId: String, signingPublicKey: ByteString)

object DataItemOwnerTable extends SQLSyntaxSupport[DataItemOwnerTable] {
  override val tableName = Schema.DataItemOwnerTableName

  def apply(ln: ResultName[DataItemOwnerTable])(rs: WrappedResultSet): DataItemOwnerTable = {
    DataItemOwnerTable(
      rs.string(ln.dataItemId),
      ByteString(rs.bytes(ln.signingPublicKey))
    )
  }

}
