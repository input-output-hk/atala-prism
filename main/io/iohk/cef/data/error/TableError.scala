package io.iohk.cef.data.error

import io.iohk.cef.crypto._
import io.iohk.cef.data.{DataItem, DataItemId, TableId}
import io.iohk.cef.error.ApplicationError

sealed trait TableError extends ApplicationError

case class InvalidSignaturesError[I](dataItem: DataItem[I], signature: Seq[Signature]) extends TableError {
  override def toString: String = s"The signatures ${signature} are invalid for dataItem ${dataItem}"
}

case class OwnerMustSignDelete[I](dataItem: DataItem[I]) extends TableError {
  override def toString: String = s"No valid owner signature was provided for dataItem ${dataItem}"
}

case class DataItemNotFound(tableId: TableId, dataItemId: DataItemId) extends TableError {
  override def toString: String = s"Data item ${dataItemId} not found on table ${tableId}"
}
