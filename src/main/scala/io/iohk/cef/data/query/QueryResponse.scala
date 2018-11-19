package io.iohk.cef.data.query
import io.iohk.cef.data.DataItem

case class QueryResponse[I](dataItems: Seq[DataItem[I]])
