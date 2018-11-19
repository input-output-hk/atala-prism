package io.iohk.cef.data.query
import java.util.UUID

import io.iohk.cef.codecs.nio._
import io.iohk.cef.core.{Envelope, Everyone}
import io.iohk.cef.data.{DataItem, Table, TableId}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.Network

import scala.concurrent.duration.FiniteDuration

class QueryEngine(table: Table, timeout: FiniteDuration, requestNetwork: Network[Envelope[QueryRequest]]) {

  def process[I](tableId: TableId, query: Query)(
      implicit itemSerializable: NioEncDec[I]): Either[ApplicationError, Seq[DataItem[I]]] = {
    for {
      network <- processInNetwork[I](tableId, query)
      local <- processLocally(tableId, query)
    } yield local ++ network
  }

  private def processInNetwork[I](tableId: TableId, query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {
    requestNetwork.disseminateMessage(Envelope(QueryRequest(UUID.randomUUID().toString, query), tableId, Everyone))
    requestNetwork.messageStream.
  }

  private def processLocally[I](tableId: TableId, query: Query)(
      implicit itemSerializable: NioEncDec[I]): Either[ApplicationError, Seq[DataItem[I]]] = {
    table.select(tableId, query)
  }
}
