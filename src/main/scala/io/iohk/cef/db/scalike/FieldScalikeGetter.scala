package io.iohk.cef.db.scalike
import io.iohk.cef.data.DataItem
import io.iohk.cef.data.query.{Field, FieldGetter}
import io.iohk.cef.data.storage.scalike.DataItemTable
import scalikejdbc._

object FieldScalikeGetter {

  def dataItemFieldGetter[T](
      di: QuerySQLSyntaxProvider[SQLSyntaxSupport[DataItemTable], DataItemTable]): FieldGetter[SQLSyntax] =
    (field: Field) => {
      field.index match {
        case DataItem.FieldIds.DataItemId => di.dataItemId
        case DataItem.FieldIds.DataTableId => di.dataTableId
      }
    }
}
