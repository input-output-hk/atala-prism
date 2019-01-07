package io.iohk.cef.data.query
import io.iohk.cef.data.DataItem

case class DataItemQueryResponse[I](queryId: String, result: Either[DataItemQueryError, Seq[DataItem[I]]])
