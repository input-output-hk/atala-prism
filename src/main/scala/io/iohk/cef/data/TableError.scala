package io.iohk.cef.data
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.crypto._

sealed trait TableError extends ApplicationError

class InvalidSignaturesError(dataItem: DataItem, signature: Seq[Signature]) extends TableError {
  override def toString: TableId = s"The signatures ${signature} are invalid for dataItem ${dataItem}"
}

class OwnerMustSignDelete(dataItem: DataItem) extends TableError {
  override def toString: TableId = s"No valid owner signature was provided for dataItem ${dataItem}"
}
