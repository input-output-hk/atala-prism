package io.iohk.cef.integration

import io.iohk.cef.ledger._
import io.iohk.cef.ledger.query.LedgerQueryService
import io.iohk.cef.test.{DummyLedgerQuery, DummyTransaction}
import io.iohk.cef.transactionservice.NodeTransactionServiceImpl
import io.iohk.network._
import monix.execution.Ack
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._

class TransactionServiceNetworkItSpec
    extends FlatSpec
    with MustMatchers
    with PropertyChecks
    with NetworkFixture
    with MockitoSugar {

  behavior of "TransactionServiceNetworkItSpec"
  import io.iohk.codecs.nio.auto._

  implicit val executionContext = scala.concurrent.ExecutionContext.global

  val txNetwork = 1

  private def createTransactionService(
      baseNetwork: BaseNetwork,
      me: NodeId,
      proposedTransactionsSubject: ProposedTransactionsSubject[DummyTransaction],
      proposedBlocksSubject: ProposedBlocksSubject[String, DummyTransaction]
  ) = {
    val txNetwork = Network[Envelope[DummyTransaction]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val blockNetwork =
      Network[Envelope[Block[String, DummyTransaction]]](baseNetwork.networkDiscovery, baseNetwork.transports)
    val queryService = mock[LedgerQueryService[String, DummyLedgerQuery]]
    val consensusMap = Map(
      "1" ->
        new LedgerServices(
          proposedTransactionsSubject = proposedTransactionsSubject,
          proposedBlocksSubject = proposedBlocksSubject,
          appliedBlocksSubject = mock[AppliedBlocksSubject[String, DummyTransaction]],
          queryService
        )
    )

    new NodeTransactionServiceImpl[String, DummyTransaction, DummyLedgerQuery](
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
    val mockTransactionChannel1 = mock[ProposedTransactionsSubject[DummyTransaction]]
    val mockTransactionChannel2 = mock[ProposedTransactionsSubject[DummyTransaction]]

    val proposedBlocksSubject1 = mock[ProposedBlocksSubject[String, DummyTransaction]]
    val proposedBlocksSubject2 = mock[ProposedBlocksSubject[String, DummyTransaction]]

    val transactionservice1 =
      createTransactionService(
        baseNetworkTransactionService1,
        NodeId("1111"),
        mockTransactionChannel1,
        proposedBlocksSubject1
      )
    val transactionservice2 =
      createTransactionService(
        baseNetworkTransactionService2,
        NodeId("2222"),
        mockTransactionChannel2,
        proposedBlocksSubject2
      )

    val testTx = DummyTransaction(10)
    when(mockTransactionChannel2.onNext(testTx)).thenReturn(Ack.Continue)
    when(mockTransactionChannel1.onNext(testTx)).thenReturn(Ack.Continue)
    val transactionservice2ProcessesTx = transactionservice2.receiveTransaction(Envelope(testTx, "1", Everyone))
    Await.result(transactionservice2ProcessesTx, 1 minute) mustBe Right(())
    verify(mockTransactionChannel1, timeout(5000).times(1)).onNext(testTx)

    val testBlock = Block[String, DummyTransaction](BlockHeader(), immutable.Seq(testTx))

    val transactionservice2ProcessesBlock = transactionservice2.receiveBlock(Envelope(testBlock, "1", Everyone))
    Await.result(transactionservice2ProcessesBlock, 1 minute) mustBe Right(())
    verify(proposedBlocksSubject1, timeout(5000).times(1)).onNext(testBlock)
  }
}
