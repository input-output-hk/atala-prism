package io.iohk.cef.integration

import io.iohk.cef.consensus.Consensus
import io.iohk.cef.transactionservice.{Envelope, Everyone, NodeTransactionService}
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.network.{MessageStream, Network, NodeId}
import io.iohk.cef.test.DummyTransaction
import io.iohk.cef.transactionpool.TransactionPoolInterface
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, _}
import io.iohk.cef.codecs.nio.auto._

class TransactionServicePooltSpec extends FlatSpecLike with MustMatchers with BeforeAndAfterAll with MockitoSugar {

  private def mockLedgerStateStorage[State] = mock[LedgerStateStorage[State]]

  behavior of "TransactionServicePoolItSpec"

  it should "process a transaction" in {
    implicit val executionContext = ExecutionContext.global
    val ledgerStateStorage = mockLedgerStateStorage[String]
    val generateHeader: Seq[Transaction[String]] => BlockHeader = _ => BlockHeader()
    val transactionPoolFutureInterface =
      TransactionPoolInterface[String, DummyTransaction](
        generateHeader,
        3,
        ledgerStateStorage,
        1 minute
      )
    val consensus = mock[Consensus[String, DummyTransaction]]
    val txNetwork = mock[Network[Envelope[DummyTransaction]]]
    val blockNetwork = mock[Network[Envelope[Block[String, DummyTransaction]]]]
    val consensusMap = Map("1" -> (transactionPoolFutureInterface, consensus))
    val me = NodeId("3112")
    val mockTxMessageStream = mock[MessageStream[Envelope[DummyTransaction]]]
    val mockBlockMessageStream =
      mock[MessageStream[Envelope[Block[String, DummyTransaction]]]]
    when(txNetwork.messageStream).thenReturn(mockTxMessageStream)
    when(blockNetwork.messageStream).thenReturn(mockBlockMessageStream)
    when(mockTxMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    when(mockBlockMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    val transactionservice = new NodeTransactionService(consensusMap, txNetwork, blockNetwork, me)
    val testTransaction = DummyTransaction(5)
    val envelope = Envelope(testTransaction, "1", Everyone)
    val result = Await.result(transactionservice.receiveTransaction(envelope), 10 seconds)
    result mustBe Right(())
    val resultBlock = transactionPoolFutureInterface.generateBlock()
    resultBlock mustBe Right(Block[String, DummyTransaction](BlockHeader(), Queue(DummyTransaction(5))))
    resultBlock.map {
      _.transactions mustBe Seq(testTransaction)
    }
  }
}
