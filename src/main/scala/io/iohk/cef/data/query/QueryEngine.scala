package io.iohk.cef.data.query
import java.util.UUID

import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.{DataItem, Table, TableId}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{Network, NodeId}
import io.iohk.cef.transactionservice.{Envelope, Everyone}
import io.iohk.cef.utils.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

class QueryEngine[I](
    nodeId: NodeId,
    table: Table,
    timeout: FiniteDuration,
    requestNetwork: Network[Envelope[QueryRequest]],
    responseNetwork: Network[QueryResponse[I]])(
    implicit itemSerializable: NioEncDec[I],
    executionContext: ExecutionContext)
    extends Logger {

  def process(tableId: TableId, query: Query): Future[Either[ApplicationError, Seq[DataItem[I]]]] = {
    val networkFuture = networkProcessing(tableId, query)
    networkFuture.map(networkEither => {
      for {
        local <- localProcessing(tableId, query)
        network <- networkEither
      } yield local ++ network
    })
  }

  private def networkProcessing(tableId: TableId, query: Query): Future[Either[ApplicationError, Seq[DataItem[I]]]] = {
    val queryId = UUID.randomUUID().toString
    requestNetwork.disseminateMessage(Envelope(QueryRequest(queryId, query, nodeId), tableId, Everyone))
    val responses = responseNetwork.messageStream
      .filter(_.queryId == queryId)
      .map(_.result)
      .fold[Either[ApplicationError, Seq[DataItem[I]]]](Right(Seq()))((state, current) => {
        for {
          s <- state
          c <- current
        } yield c ++ s
      })
    responses
  }

  private def localProcessing(tableId: TableId, query: Query): Either[ApplicationError, Seq[DataItem[I]]] = {
    table.select[I](tableId, query)
  }

  private def processFromNetwork(envelope: Envelope[QueryRequest]): Unit = {
    val localQueryResult = localProcessing(envelope.containerId, envelope.content.query)
    responseNetwork.sendMessage(envelope.content.replyTo, QueryResponse[I](envelope.content.id, localQueryResult))
  }
}
