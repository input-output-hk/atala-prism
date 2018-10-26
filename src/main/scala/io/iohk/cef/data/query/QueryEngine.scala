package io.iohk.cef.data.query
import io.iohk.cef.data.{DataItem, DataItemFactory, Table}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.network.NetworkFacade

import scala.concurrent.duration.FiniteDuration

class QueryEngine(networkFacade: NetworkFacade, table: Table, timeout: FiniteDuration) {

  def process[I, U](query: Query[DataItem[I], U])(
      implicit dataItemFactory: DataItemFactory[I],
      itemSerializable: ByteStringSerializable[I]): Either[ApplicationError, Seq[U]] = {
    for {
      local <- processLocally(query)
      network <- processNetwork[DataItem[I], U]
    } yield local ++ network
  }

  private def processNetwork[I, U]: Either[ApplicationError, Seq[U]] = ???

  private def processLocally[I, U](query: Query[DataItem[I], U])(
      implicit dataItemFactory: DataItemFactory[I],
      itemSerializable: ByteStringSerializable[I]): Either[ApplicationError, Seq[U]] = {
    for {
      elements <- table.select(query.tableId)
    } yield elements.filter(query.select.predicate).map(query.projection.f)
  }
}
