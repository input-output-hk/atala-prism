package io.iohk.cef.data.storage.scalike
import akka.util.ByteString
import scalikejdbc._

case class DataItemTable(
    id: String,
    dataItem: ByteString)

object DataItemTable extends SQLSyntaxSupport[DataItemTable] {
  override val tableName = Schema.DataTableName

  def appy(ln: ResultName[DataItemTable])(rs: WrappedResultSet): DataItemTable = {
    DataItemTable(
      rs.string(ln.id),
      ByteString(rs.bytes(ln.dataItem))
    )
  }
}
