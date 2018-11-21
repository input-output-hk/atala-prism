package io.iohk.cef.data.query
import io.iohk.cef.data.DataItem
import io.iohk.cef.error.ApplicationError

case class QueryResponse[I](queryId: String, result: Either[ApplicationError, Seq[DataItem[I]]])
