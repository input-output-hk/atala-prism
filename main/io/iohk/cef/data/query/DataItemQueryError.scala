package io.iohk.cef.data.query
import io.iohk.cef.data.TableId
import io.iohk.cef.error.ApplicationError

sealed trait DataItemQueryError extends ApplicationError

case class InvalidDataItemQueryError(tableId: TableId, field: Field) extends DataItemQueryError {
  override def toString: String = s"Error in query. Unable to access field '$field' on table '$tableId'."
}
