package io.iohk.cef.integration
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.core.{Envelope, Everyone, NodeCore}
import io.iohk.cef.ledger.{Block, ByteStringSerializable}
import io.iohk.cef.network.{Network, NetworkFixture, NodeId}
import io.iohk.cef.test.{DummyBlockHeader, DummyTransaction}
import io.iohk.cef.transactionpool.TransactionPoolFutureInterface
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class CoreNetworkItSpec extends FlatSpec with MustMatchers with PropertyChecks with NetworkFixture with MockitoSugar {

  def mockConsensus: Consensus[String, DummyBlockHeader, DummyTransaction] =
    mock[Consensus[String, DummyBlockHeader, DummyTransaction]]

  def mockTxPoolFutureInterface: TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction] =
    mock[TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction]]

  behavior of "CoreNetworkItSpec"
  import io.iohk.cef.network.encoding.nio.NioCodecs._
  import io.iohk.cef.test.DummyBlockSerializable._

  implicit val executionContext = scala.concurrent.ExecutionContext.global

  val txNetwork = 1
  implicit val nioEncoder: NioEncoder[Envelope[DummyTransaction]] =
    implicitly[ByteStringSerializable[Envelope[DummyTransaction]]].toNioEncoder
  implicit val nioDecoder: NioDecoder[Envelope[DummyTransaction]] =
    implicitly[ByteStringSerializable[Envelope[DummyTransaction]]].toNioDecoder
  implicit val blockNioEncoder: NioEncoder[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]] =
    implicitly[ByteStringSerializable[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]].toNioEncoder
  implicit val blockNioDecoder: NioDecoder[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]] =
    implicitly[ByteStringSerializable[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]].toNioDecoder

  private def createCore(
      baseNetwork: BaseNetwork,
      me: NodeId,
      txPoolIf: TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction],
      consensus: Consensus[String, DummyBlockHeader, DummyTransaction]) = {
    val txNetwork = new Network[Envelope[DummyTransaction]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val blockNetwork =
      new Network[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]](
        baseNetwork.networkDiscovery,
        baseNetwork.transports)
    val consensusMap = Map(1 -> (txPoolIf, consensus))

    new NodeCore[String, DummyBlockHeader, DummyTransaction](
      consensusMap,
      txNetwork,
      blockNetwork,
      baseNetwork.transports.peerInfo.nodeId
    )
  }

  private val bootstrap = randomBaseNetwork(None)
  it should "receive a transaction and a block" in networks(bootstrap, randomBaseNetwork(Some(bootstrap))) { networks =>
    val baseNetworkCore1 = networks(0)
    val baseNetworkCore2 = networks(1)
    val mockTxPoolIf1 = mockTxPoolFutureInterface
    val mockTxPoolIf2 = mockTxPoolFutureInterface
    val mockCons1 = mockConsensus
    val mockCons2 = mockConsensus

    val core1 = createCore(baseNetworkCore1, NodeId("1111"), mockTxPoolIf1, mockCons1)
    val core2 = createCore(baseNetworkCore2, NodeId("2222"), mockTxPoolIf2, mockCons2)

    val testTx = DummyTransaction(10)
    when(mockTxPoolIf2.processTransaction(testTx)).thenReturn(Future.successful(Right(())))
    when(mockTxPoolIf1.processTransaction(testTx)).thenReturn(Future.successful(Right(())))
    val core2ProcessesTx = core2.receiveTransaction(Envelope(testTx, 1, Everyone))
    Await.result(core2ProcessesTx, 1 minute) mustBe Right(())
    verify(mockTxPoolIf1, timeout(5000).times(1)).processTransaction(testTx)

    val testBlock = Block(DummyBlockHeader(10), immutable.Seq(testTx))

    when(mockCons1.process(testBlock)).thenReturn(Future.successful(Right(())))
    when(mockCons2.process(testBlock)).thenReturn(Future.successful(Right(())))
    val core2ProcessesBlock = core2.receiveBlock(Envelope(testBlock, 1, Everyone))
    Await.result(core2ProcessesBlock, 1 minute) mustBe Right(())
    verify(mockCons1, timeout(5000).times(1)).process(testBlock)
  }
}
