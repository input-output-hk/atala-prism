package io.iohk.cef.data
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.crypto._

sealed trait TableError extends ApplicationError

class InvalidSignaturesError[I](dataItem: DataItem[I], signature: Seq[Signature]) extends TableError {
  override def toString: DataItemId = s"The signatures ${signature} are invalid for dataItem ${dataItem}"
}

class OwnerMustSignDelete[I](dataItem: DataItem[I]) extends TableError {
  override def toString: DataItemId = s"No valid owner signature was provided for dataItem ${dataItem}"
}
