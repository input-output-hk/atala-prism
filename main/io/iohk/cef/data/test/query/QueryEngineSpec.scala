package io.iohk.cef.data.query

import io.iohk.cef.data._
import io.iohk.cef.data.query.DataItemQuery.NoPredicateDataItemQuery
import io.iohk.cef.test.DummyMessageStream
import io.iohk.cef.utils.NonEmptyList
import io.iohk.codecs.nio.auto._
import io.iohk.crypto._
import io.iohk.network.{Envelope, Everyone, Network, NodeId}
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
    val data = "dataItem1"
    val keys = generateSigningKeyPair()
    val owner = Owner(keys.public, sign(LabeledItem.Create(data), keys.`private`))
    val queryResult = Seq(DataItem(data, Seq(), NonEmptyList(owner)))
    val queries = Seq(NoPredicateDataItemQuery)
    val queryResponse = DataItemQueryResponse(queryId, Right(queryResult))
    val queryResponsesEnvelope = Envelope(queryResponse, tableId, Everyone)

    val queryIdsIterator = Seq(queryId).iterator
    implicit val scheduler = TestScheduler()
    val envResponsesIt = Seq(queryResponsesEnvelope).iterator
    val observableRequest: Observable[Envelope[DataItemQueryRequest]] = Observable.empty
    val observableResponse: Observable[Envelope[DataItemQueryResponse[String]]] = Observable.eval(envResponsesIt.next())

    val requestMessageStream = new DummyMessageStream[Envelope[DataItemQueryRequest]](observableRequest)
    val requestNetwork = mock[Network[Envelope[DataItemQueryRequest]]]
    val responseNetwork: Network[Envelope[DataItemQueryResponse[String]]] =
      mock[Network[Envelope[DataItemQueryResponse[String]]]]
    val responseMessageStream = new DummyMessageStream[Envelope[DataItemQueryResponse[String]]](observableResponse)

    when(table.tableId).thenReturn(tableId)
    when(requestNetwork.messageStream).thenReturn(requestMessageStream)
    when(responseNetwork.messageStream).thenReturn(responseMessageStream)

    val engine = new DataItemQueryEngine(nodeId, table, requestNetwork, responseNetwork, () => queryIdsIterator.next())
    val query = queries.head
    when(table.select(query)).thenReturn(Right(Seq()))
    val streamResult = engine.process(query)
    //verify query was disseminated
    verify(requestNetwork, times(1))
      .disseminateMessage(Envelope(DataItemQueryRequest("query1", query, nodeId), tableId, Everyone))
    verify(table, times(1)).select(query)

    val validation: Seq[DataItem[String]] => Unit = mock[Seq[DataItem[String]] => Unit]
    streamResult.foreach(either => either.map(response => validation(response)))
    eventually { verify(validation, times(1)).apply(queryResult) }
  }
}
