package io.iohk.cef.data.query
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.{DataItem, Table, TableId}
import io.iohk.cef.error.ApplicationError

import scala.concurrent.duration.FiniteDuration
import scala.reflect.runtime.universe.TypeTag

class QueryEngine(table: Table, timeout: FiniteDuration) {

  def process[I: NioEncDec: TypeTag](tableId: TableId, query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {
    for {
      network <- processNetwork[I]
      local <- processLocally(tableId, query)
    } yield local ++ network
  }

  private def processNetwork[I]: Either[ApplicationError, Seq[DataItem[I]]] =
    ??? //Send query, wait for answers or timeout whichever is first

  private def processLocally[I: NioEncDec: TypeTag](
      tableId: TableId,
      query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {
    table.select(tableId, query)
  }
}
