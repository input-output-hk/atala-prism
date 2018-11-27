package io.iohk.cef.data.query
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.{DataItem, Table, TableId}
import io.iohk.cef.error.ApplicationError

import scala.concurrent.duration.FiniteDuration
import scala.reflect.runtime.universe.TypeTag

class QueryEngine[I: NioEncDec: TypeTag](table: Table[I], timeout: FiniteDuration) {

  def process(tableId: TableId, query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {
    for {
      network <- processNetwork
      local <- processLocally(query)
    } yield local ++ network
  }

  private def processNetwork: Either[ApplicationError, Seq[DataItem[I]]] =
    ??? //Send query, wait for answers or timeout whichever is first

  private def processLocally(query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {
    table.select(query)
  }
}
