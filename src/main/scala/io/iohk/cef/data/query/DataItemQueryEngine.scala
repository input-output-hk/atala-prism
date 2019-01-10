package io.iohk.cef.data.query
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.{DataItem, Table}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network, NodeId}
import io.iohk.cef.transactionservice.{Envelope, Everyone}
import io.iohk.cef.utils.Logger

class DataItemQueryEngine[I](
    nodeId: NodeId,
    table: Table[I],
    requestNetwork: Network[Envelope[DataItemQueryRequest]],
    responseNetwork: Network[Envelope[DataItemQueryResponse[I]]],
    queryIdGenerator: () => String
)(implicit itemSerializable: NioCodec[I])
    extends Logger {

  requestNetwork.messageStream.foreach(processFromNetwork)

  def process(
      query: DataItemQuery
  )(implicit itemSerializable: NioCodec[I]): MessageStream[Either[ApplicationError, Seq[DataItem[I]]]] = {
    val responseStream = networkProcessing(query)
    val localProcessingEither = localProcessing(query)
    responseStream.prepend(localProcessingEither)
  }

  private def networkProcessing(query: DataItemQuery): MessageStream[Either[ApplicationError, Seq[DataItem[I]]]] = {
    val queryId = queryIdGenerator()
    requestNetwork.disseminateMessage(Envelope(DataItemQueryRequest(queryId, query, nodeId), table.tableId, Everyone))
    responseNetwork.messageStream
      .filter(_.content.queryId == queryId)
      .map(_.content.result)
  }

  private def localProcessing(
      query: DataItemQuery
  )(implicit itemSerializable: NioCodec[I]): Either[DataItemQueryError, Seq[DataItem[I]]] = {
    table.select(query)
  }

  private def processFromNetwork(envelope: Envelope[DataItemQueryRequest]): Unit = {
    val localQueryResult = localProcessing(envelope.content.query)
    responseNetwork.sendMessage(
      envelope.content.replyTo,
      Envelope(DataItemQueryResponse[I](envelope.content.id, localQueryResult), envelope.containerId, Everyone)
    )
  }
}
