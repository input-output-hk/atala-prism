package io.iohk.cef.data.query
import io.iohk.cef.data.DataItem

case class QueryResponse[I](queryId: String, result: Either[QueryError, Seq[DataItem[I]]])
