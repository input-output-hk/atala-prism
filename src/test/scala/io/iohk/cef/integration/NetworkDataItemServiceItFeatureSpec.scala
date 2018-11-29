package io.iohk.cef.integration

import io.iohk.cef.codecs.nio._
import io.iohk.cef.transactionservice.{DestinationDescriptor, Envelope, Everyone}
import io.iohk.cef.data._
import io.iohk.cef.network.{Network, NetworkFixture}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FeatureSpec, GivenWhenThen, MustMatchers}
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.data.DataItemAction.InsertAction
import io.iohk.cef.data.query.QueryEngine
import org.mockito.ArgumentMatchers._

class NetworkDataItemServiceItFeatureSpec
    extends FeatureSpec
    with GivenWhenThen
    with MustMatchers
    with NetworkFixture
    with MockitoSugar {
  private implicit val executionContext = scala.concurrent.ExecutionContext.global
  private val bootstrap = randomBaseNetwork(None)

  private def createDataItemService(table: Table[String], baseNetwork: BaseNetwork)(
      implicit actionEncDec: NioCodec[DataItemAction[String]],
      destinationDescriptorEncDec: NioCodec[DestinationDescriptor],
      itemEncDec: NioCodec[DataItem[String]],
      canValidate: CanValidate[DataItem[String]],
      frameCodec: NioCodec[Envelope[DataItemAction[String]]]) = {

    val txNetwork = new Network[Envelope[DataItemAction[String]]](baseNetwork.networkDiscovery, baseNetwork.transports)
    new DataItemService[String](table, txNetwork, mock[QueryEngine[String]])
  }

  feature("Network DataItemService Integration") {
    scenario("Insert DataItem into the table on the network") {
      Given("a network created with 2 nodes")
      networks(bootstrap, randomBaseNetwork(Some(bootstrap))) { networks =>
        val baseNetworkNode1 = networks(0)
        val baseNetworkNode2 = networks(1)

        When("the DataItemService created for the network")

        val table = mock[Table[String]]
        val table2 = mock[Table[String]]
        implicit val canValidate: CanValidate[DataItem[String]] = (t: DataItem[String]) => Right(())

        val dataItemService = createDataItemService(table, baseNetworkNode1)
        createDataItemService(table2, baseNetworkNode2)

        when(table.insert(any())(any())).thenReturn(Right(()))

        Then("the DataItemService should insert the table on the network 1")

        val input: Envelope[DataItemAction[String]] = setUpInsertData()

        dataItemService.processAction(input) mustBe Right(())

        And("the  DataItemService insert the table on the network 2")

        verify(table2, timeout(5000).times(1)).insert(any())(any())
      }
    }

  }

  private def setUpInsertData(): Envelope[DataItemAction[String]] = {
    val data = "test-data"
    val itemId = "item-id"
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
