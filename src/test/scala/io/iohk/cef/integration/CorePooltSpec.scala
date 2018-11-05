package io.iohk.cef.integration
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.core.{Envelope, Everyone, NodeCore}
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.network.{MessageStream, Network, NodeId}
import io.iohk.cef.test.{DummyBlockHeader, DummyTransaction}
import io.iohk.cef.transactionpool.{TimedQueue, TransactionPoolInterface}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, _}
import io.iohk.cef.codecs.nio.auto._

class CorePooltSpec extends FlatSpecLike with MustMatchers with BeforeAndAfterAll with MockitoSugar {

  private def mockLedgerStateStorage[State] = mock[LedgerStateStorage[State]]

  import io.iohk.cef.ledger.ByteSizeableImplicits._

  behavior of "CorePoolItSpec"

  it should "process a transaction" in {
    implicit val executionContext = ExecutionContext.global
    import DummyTransaction._
    val ledgerStateStorage = mockLedgerStateStorage[String]
    val queue = TimedQueue[DummyTransaction]()
    val transactionPoolFutureInterface =
      new TransactionPoolInterface[String, DummyBlockHeader, DummyTransaction](
        txs => new DummyBlockHeader(txs.size),
        10000,
        ledgerStateStorage,
        1 minute,
        () => queue
      )
    val consensus = mock[Consensus[String, DummyBlockHeader, DummyTransaction]]
    val txNetwork = mock[Network[Envelope[DummyTransaction]]]
    val blockNetwork = mock[Network[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]]
    val consensusMap = Map("1" -> (transactionPoolFutureInterface, consensus))
    val me = NodeId("3112")
    val mockTxMessageStream = mock[MessageStream[Envelope[DummyTransaction]]]
    val mockBlockMessageStream =
      mock[MessageStream[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]]
    when(txNetwork.messageStream).thenReturn(mockTxMessageStream)
    when(blockNetwork.messageStream).thenReturn(mockBlockMessageStream)
    when(mockTxMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    when(mockBlockMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    val core = new NodeCore(consensusMap, txNetwork, blockNetwork, me)
    val testTransaction = DummyTransaction(5)
    val envelope = Envelope(testTransaction, "1", Everyone)
    val result = Await.result(core.receiveTransaction(envelope), 10 seconds)
    result mustBe Right(())
    val resultBlock = transactionPoolFutureInterface.generateBlock()
    resultBlock mustBe Right(
      Block[String, DummyBlockHeader, DummyTransaction](DummyBlockHeader(1), Queue(DummyTransaction(5))))
    resultBlock.map {
      _.transactions mustBe Seq(testTransaction)
    }
  }
}
