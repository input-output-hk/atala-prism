package io.iohk.cef.data.query
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.{DataItem, Table, TableId}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network, NodeId}
import io.iohk.cef.transactionservice.{Envelope, Everyone}
import io.iohk.cef.utils.Logger

class QueryEngine[I](
    nodeId: NodeId,
    table: Table,
    requestNetwork: Network[Envelope[QueryRequest]],
    responseNetwork: Network[Envelope[QueryResponse[I]]],
    queryIdGenerator: () => String)(
    implicit itemSerializable: NioEncDec[I])
    extends Logger {

  requestNetwork.messageStream.foreach(processFromNetwork)

  def process(tableId: TableId, query: Query)(
    implicit itemSerializable: NioEncDec[I]): MessageStream[Either[ApplicationError, Seq[DataItem[I]]]] = {
    val responseStream = networkProcessing(tableId, query)
    val localProcessingEither = localProcessing(tableId, query)
    responseStream.map(networkEither => {
      for {
        local <- localProcessingEither
        network <- networkEither
      } yield local ++ network
    })
  }

  private def networkProcessing(tableId: TableId, query: Query): MessageStream[Either[ApplicationError, Seq[DataItem[I]]]] = {
    val queryId = queryIdGenerator()
    requestNetwork.disseminateMessage(Envelope(QueryRequest(queryId, query, nodeId), tableId, Everyone))
    responseNetwork.messageStream
      .filter(_.content.queryId == queryId)
      .map(_.content.result)
  }

  private def localProcessing(tableId: TableId, query: Query)(
    implicit itemSerializable: NioEncDec[I]): Either[ApplicationError, Seq[DataItem[I]]] = {
    table.select[I](tableId, query)
  }

  private def processFromNetwork(envelope: Envelope[QueryRequest]): Unit = {
    val localQueryResult = localProcessing(envelope.containerId, envelope.content.query)
    responseNetwork.sendMessage(envelope.content.replyTo, Envelope(QueryResponse[I](envelope.content.id, localQueryResult), envelope.containerId, Everyone))
  }
}
