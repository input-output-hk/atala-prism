package io.iohk.cef.integration

import java.util.UUID

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.data.DataItemAction.InsertAction
import io.iohk.cef.data._
import io.iohk.cef.data.query.{Field, QueryEngine, QueryRequest, QueryResponse}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{Network, NetworkFixture}
import io.iohk.cef.transactionservice.{DestinationDescriptor, Envelope, Everyone}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen, MustMatchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class QueryEngineNetworkDataItemServiceItFeatureSpec
    extends FeatureSpec
    with GivenWhenThen
    with MustMatchers
    with NetworkFixture
    with MockitoSugar {
  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private val bootstrap = randomBaseNetwork(None)

  private def createDataItemService(table: Table[String], baseNetwork: BaseNetwork)(
      implicit enc: NioEncDec[String],
      actionEncDec: NioEncDec[DataItemAction[String]],
      destinationDescriptorEncDec: NioEncDec[DestinationDescriptor],
      itemEncDec: NioEncDec[DataItem[String]],
      canValidate: CanValidate[DataItem[String]],
      frameCodec: NioEncDec[Envelope[DataItemAction[String]]]) = {

    val txNetwork = Network[Envelope[DataItemAction[String]]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val queryRequestNetwork = Network[Envelope[QueryRequest]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val queryResponseNetwork = Network[Envelope[QueryResponse[String]]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val queryEngine =
      new QueryEngine[String](baseNetwork.transports.peerConfig.nodeId,
        table,
        queryRequestNetwork,
        queryResponseNetwork,
        () => UUID.randomUUID().toString
      )
    new DataItemService[String](table, txNetwork, queryEngine)
  }

  feature("Network DataItemService QueryEngine Integration") {
    scenario("Query DataItem after insertion into the table on the network") {
      Given("a network created with 2 nodes")
      networks(bootstrap, randomBaseNetwork(Some(bootstrap))) { networks =>
        val baseNetworkNode1 = networks(0)
        val baseNetworkNode2 = networks(1)

        When("the DataItemService created for the network")

        val table = mock[Table[String]]
        when(table.tableId).thenReturn("table1")
        val table2 = mock[Table[String]]
        when(table.tableId).thenReturn("table2")
        implicit val canValidate: CanValidate[DataItem[String]] = (t: DataItem[String]) => Right(())

        val dataItemService = createDataItemService(table, baseNetworkNode1)
        createDataItemService(table2, baseNetworkNode2)

        when(table.insert(any())(any())).thenReturn(Right(()))

        Then("the DataItemService should insert the table on the network 1")

        val itemId = "item-id"
        val input: Envelope[DataItemAction[String]] = setUpInsertData(itemId)

        dataItemService.processAction(input) mustBe Right(())

        And("the  DataItemService insert the table on the network 2")

        verify(table2, timeout(5000).times(1)).insert(any())(any())

        Then("the DataItemService should return the data item with a query")

        val query = Field(0) #== itemId

        val dummyResultDataItem1 = DataItem[String]("id1","data1",Seq(),Seq())
        val dummyResultDataItem2 = DataItem[String]("id2","data2",Seq(),Seq())

        when(table.select(query)).thenReturn(Right(Seq(dummyResultDataItem1)))
        when(table2.select(query)).thenReturn(Right(Seq(dummyResultDataItem2)))

        val stream =
          dataItemService.processQuery(Envelope(query, table.tableId, Everyone)).withTimeout(1 seconds)

        val responses = Await.result(stream.fold[Either[ApplicationError, Seq[DataItem[String]]]](Right(Seq()))((s, c) => for {
          seq <- c
          state <- s
        } yield state ++ seq), 30 seconds)

        responses mustBe
          Right(Seq(dummyResultDataItem1, dummyResultDataItem2))

        And("the query was run on both nodes")

        verify(table, times(1)).select(query)
        verify(table2, times(1)).select(query)
      }
    }

  }

  private def setUpInsertData(itemId: DataItemId): Envelope[DataItemAction[String]] = {
    val data = "test-data"
    val containerId = "1"
    val dataItem = DataItem[String](itemId, data, Seq.empty[Witness], Seq.empty[Owner])
    val insert: DataItemAction[String] = InsertAction(dataItem)
    val input = Envelope(
      content = insert,
      containerId = containerId,
      destinationDescriptor = Everyone
    )
    input
  }
}
