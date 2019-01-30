package io.iohk.cef.integration
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.{Block, BlockHeader}
import io.iohk.cef.network._
import io.iohk.cef.test.DummyTransaction
import io.iohk.cef.transactionpool.TransactionPoolInterface
import io.iohk.cef.transactionservice.NodeTransactionService
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TransactionServiceNetworkItSpec
    extends FlatSpec
    with MustMatchers
    with PropertyChecks
    with NetworkFixture
    with MockitoSugar {

  def mockConsensus: Consensus[String, DummyTransaction] =
    mock[Consensus[String, DummyTransaction]]

  def mockTxPoolFutureInterface: TransactionPoolInterface[String, DummyTransaction] =
    mock[TransactionPoolInterface[String, DummyTransaction]]

  behavior of "TransactionServiceNetworkItSpec"
  import io.iohk.cef.codecs.nio.auto._

  implicit val executionContext = scala.concurrent.ExecutionContext.global

  val txNetwork = 1

  private def createTransactionService(
      baseNetwork: BaseNetwork,
      me: NodeId,
      txPoolIf: TransactionPoolInterface[String, DummyTransaction],
      consensus: Consensus[String, DummyTransaction]
  ) = {
    val txNetwork = Network[Envelope[DummyTransaction]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val blockNetwork =
      Network[Envelope[Block[String, DummyTransaction]]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val consensusMap = Map("1" -> (txPoolIf, consensus))

    new NodeTransactionService[String, DummyTransaction](
      consensusMap,
      txNetwork,
      blockNetwork,
      baseNetwork.transports.peerConfig.nodeId
    )
  }

  private val bootstrap = randomBaseNetwork(None)
  it should "receive a transaction and a block" in networks(bootstrap, randomBaseNetwork(Some(bootstrap))) { networks =>
    val baseNetworkTransactionService1 = networks(0)
    val baseNetworkTransactionService2 = networks(1)
    val mockTxPoolIf1 = mockTxPoolFutureInterface
    val mockTxPoolIf2 = mockTxPoolFutureInterface
    val mockCons1 = mockConsensus
    val mockCons2 = mockConsensus

    val transactionservice1 =
      createTransactionService(baseNetworkTransactionService1, NodeId("1111"), mockTxPoolIf1, mockCons1)
    val transactionservice2 =
      createTransactionService(baseNetworkTransactionService2, NodeId("2222"), mockTxPoolIf2, mockCons2)

    val testTx = DummyTransaction(10)
    when(mockTxPoolIf2.processTransaction(testTx)).thenReturn(Right(()))
    when(mockTxPoolIf1.processTransaction(testTx)).thenReturn(Right(()))
    val transactionservice2ProcessesTx = transactionservice2.receiveTransaction(Envelope(testTx, "1", Everyone))
    Await.result(transactionservice2ProcessesTx, 1 minute) mustBe Right(())
    verify(mockTxPoolIf1, timeout(5000).times(1)).processTransaction(testTx)

    val testBlock = Block[String, DummyTransaction](BlockHeader(), immutable.Seq(testTx))

    when(mockCons1.process(testBlock)).thenReturn(Future.successful(Right(())))
    when(mockCons2.process(testBlock)).thenReturn(Future.successful(Right(())))
    val transactionservice2ProcessesBlock = transactionservice2.receiveBlock(Envelope(testBlock, "1", Everyone))
    Await.result(transactionservice2ProcessesBlock, 1 minute) mustBe Right(())
    verify(mockCons1, timeout(5000).times(1)).process(testBlock)
  }
}
