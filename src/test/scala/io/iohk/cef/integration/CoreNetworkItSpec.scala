package io.iohk.cef.integration
import io.iohk.cef.consensus.{Consensus, MockingConsensus}
import io.iohk.cef.core.{Anyone, Envelope, NodeCore}
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.network.{Network, NetworkFixture, NodeId}
import io.iohk.cef.test.{DummyBlockHeader, DummyTransaction}
import io.iohk.cef.transactionpool.{MockingTransactionPoolFutureInterface, TransactionPoolFutureInterface}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}
import org.mockito.Mockito._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class CoreNetworkItSpec extends FlatSpec
  with MustMatchers
  with PropertyChecks
  with NetworkFixture
  with MockitoSugar
  with MockingTransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction]
  with MockingConsensus[String, DummyTransaction] {

  behavior of "CoreNetworkItSpec"
  import io.iohk.cef.network.encoding.nio.NioCodecs._
  import io.iohk.cef.test.DummyBlockSerializable._

  implicit val executionContext = scala.concurrent.ExecutionContext.global

  val txNetwork = 1
  implicit val nioEncoder: NioEncoder[Envelope[DummyTransaction]] = implicitly[ByteStringSerializable[Envelope[DummyTransaction]]].toNioEncoder
  implicit val nioDecoder: NioDecoder[Envelope[DummyTransaction]] = implicitly[ByteStringSerializable[Envelope[DummyTransaction]]].toNioDecoder
  implicit val blockNioEncoder: NioEncoder[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]] =
    implicitly[ByteStringSerializable[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]].toNioEncoder
  implicit val blockNioDecoder: NioDecoder[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]] =
    implicitly[ByteStringSerializable[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]].toNioDecoder

  private def createCore(baseNetwork: BaseNetwork,
                         me: NodeId,
                         txPoolIf: TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction],
                         consensus: Consensus[String, DummyTransaction]) = {
    val txNetwork = new Network[Envelope[DummyTransaction]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val blockNetwork =
      new Network[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val consensusMap = Map(1 -> (txPoolIf, consensus))

    new NodeCore[String, DummyBlockHeader, DummyTransaction](
      consensusMap,
      txNetwork,
      blockNetwork,
      baseNetwork.transports.peerInfo.nodeId
    )
  }

  it should "receive a transaction" in {
    val baseNetworkCore1 = randomBaseNetwork(None)
    val baseNetworkCore2 = randomBaseNetwork(Some(baseNetworkCore1))
    println(s"baseNetworkCore1: $baseNetworkCore1")
    println(s"baseNetworkCore2: $baseNetworkCore2")
    val mockTxPoolIf1 = mockTxPoolFutureInterface
    val mockTxPoolIf2 = mockTxPoolFutureInterface
    val mockCons1 = mockConsensus
    val mockCons2 = mockConsensus

    val core1 = createCore(baseNetworkCore1, NodeId("1111"), mockTxPoolIf1, mockCons1)
    val core2 = createCore(baseNetworkCore2, NodeId("2222"), mockTxPoolIf2, mockCons2)

    val testTx = DummyTransaction(10)
    when(mockTxPoolIf2.processTransaction(testTx)).thenReturn(Future.successful(Right(())))
    when(mockTxPoolIf1.processTransaction(testTx)).thenReturn(Future.successful(Right(())))
    val result = core2.receiveTransaction(Envelope(testTx, 1, Anyone()))
    Await.result(result, 1 minute) mustBe Right(())
    verify(mockTxPoolIf1, timeout(5000).times(1)).processTransaction(testTx)
  }
}
