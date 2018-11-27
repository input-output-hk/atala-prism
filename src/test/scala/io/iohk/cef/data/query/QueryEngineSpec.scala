package io.iohk.cef.data.query
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.query.Query.NoPredicateQuery
import io.iohk.cef.data.{DataItem, Table}
import io.iohk.cef.network.{Network, NodeId}
import io.iohk.cef.test.DummyMessageStream
import io.iohk.cef.transactionservice.{Envelope, Everyone}
import monix.execution.schedulers.TestScheduler
import monix.reactive.Observable
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.{FlatSpec, MustMatchers}

class QueryEngineSpec extends FlatSpec with MustMatchers {

  behavior of "QueryEngine"

  it should "process a query" in {
    val nodeId = NodeId("1111")
    val tableId = "table"
    val table = mock[Table[String]]
    val queryId = "query1"
    val queryResult = Seq(DataItem("1", "dataItem1", Seq(), Seq()))
    val queries = Seq(NoPredicateQuery)
    val queryResponse = QueryResponse(queryId, Right(queryResult))
    val queryResponsesEnvelope = Envelope(queryResponse, tableId, Everyone)

    val queryIdsIterator = Seq(queryId).iterator
    val scheduler = TestScheduler()
    val envResponsesIt = Seq(queryResponsesEnvelope).iterator
    val observableRequest: Observable[Envelope[QueryRequest]] = Observable.empty
    val observableResponse: Observable[Envelope[QueryResponse[String]]] = Observable.eval(envResponsesIt.next())

    val requestMessageStream = new DummyMessageStream[Envelope[QueryRequest]](observableRequest, scheduler)
    val requestNetwork = mock[Network[Envelope[QueryRequest]]]
    val responseNetwork: Network[Envelope[QueryResponse[String]]] = mock[Network[Envelope[QueryResponse[String]]]]
    val responseMessageStream = new DummyMessageStream[Envelope[QueryResponse[String]]](observableResponse, scheduler)
    implicit val s = mock[NioEncDec[String]]

    when(table.tableId).thenReturn(tableId)
    when(requestNetwork.messageStream).thenReturn(requestMessageStream)
    when(responseNetwork.messageStream).thenReturn(responseMessageStream)

    val engine = new QueryEngine(nodeId, table, requestNetwork, responseNetwork, () => queryIdsIterator.next())
    val query = queries.head
    when(table.select(query)).thenReturn(Right(Seq()))
    val streamResult = engine.process(query)
    //verify query was disseminated
    verify(requestNetwork, times(1))
      .disseminateMessage(Envelope(QueryRequest("query1", query, nodeId), tableId, Everyone))
    verify(table, times(1)).select(query)

    val validation: Seq[DataItem[String]] => Unit = mock[Seq[DataItem[String]] => Unit]
    streamResult.foreach(either => either.map(response => validation(response)))
    eventually { verify(validation, times(1)).apply(queryResult) }
  }
}
