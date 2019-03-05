package io.iohk.cef.integration

import io.iohk.cef.consensus.Consensus
import io.iohk.cef.ledger.query.LedgerQueryService
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerServices, Transaction}
import io.iohk.cef.test.{DummyLedgerQuery, DummyTransaction}
import io.iohk.cef.transactionpool.TransactionPoolInterface
import io.iohk.cef.transactionservice.NodeTransactionServiceImpl
import io.iohk.codecs.nio.auto._
import io.iohk.network._
import monix.reactive.MulticastStrategy
import monix.reactive.subjects.ConcurrentSubject
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.EitherValues._
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, _}

class TransactionServicePooltSpec extends FlatSpecLike with MustMatchers with BeforeAndAfterAll with MockitoSugar {

  private def mockLedgerStateStorage = mock[LedgerStateStorage[String]]

  behavior of "TransactionServicePoolItSpec"

  it should "process a transaction" in {
    type BlockType = Block[String, DummyTransaction]

    implicit val executionContext = ExecutionContext.global
    val ledgerStateStorage = mockLedgerStateStorage

    val proposedTransactionsSubject =
      ConcurrentSubject[DummyTransaction](MulticastStrategy.publish)(monix.execution.Scheduler.global)
    val proposedBlocksSubject =
      ConcurrentSubject[BlockType](MulticastStrategy.publish)(monix.execution.Scheduler.global)
    val appliedBlocksSubject = ConcurrentSubject[BlockType](MulticastStrategy.publish)(monix.execution.Scheduler.global)

    val generateHeader: Seq[Transaction[String]] => BlockHeader = _ => BlockHeader()
    val transactionPoolFutureInterface =
      TransactionPoolInterface.apply[String, DummyTransaction](
        generateHeader,
        3,
        ledgerStateStorage,
        1 minute,
        proposedTransactionsSubject
      )

    val consensus = mock[Consensus[String, DummyTransaction]]
    val txNetwork = mock[Network[Envelope[DummyTransaction]]]
    val blockNetwork = mock[Network[Envelope[Block[String, DummyTransaction]]]]
    val queryService = mock[LedgerQueryService[String, DummyLedgerQuery]]
    val consensusMap = Map(
      "1" -> new LedgerServices(
        proposedTransactionsSubject = proposedTransactionsSubject,
        proposedBlocksSubject = proposedBlocksSubject,
        appliedBlocksSubject = appliedBlocksSubject,
        queryService
      )
    )

    val me = NodeId("3112")
    val mockTxMessageStream = mock[MessageStream[Envelope[DummyTransaction]]]
    val mockBlockMessageStream =
      mock[MessageStream[Envelope[Block[String, DummyTransaction]]]]
    when(txNetwork.messageStream).thenReturn(mockTxMessageStream)
    when(blockNetwork.messageStream).thenReturn(mockBlockMessageStream)
    when(mockTxMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    when(mockBlockMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))

    val transactionservice = new NodeTransactionServiceImpl(consensusMap, txNetwork, blockNetwork, me)
    val testTransaction = DummyTransaction(5)
    val envelope = Envelope(testTransaction, "1", Everyone)
    val result = Await.result(transactionservice.receiveTransaction(envelope), 10 seconds)
    result mustBe Right(())

    eventually {
      val resultBlock = transactionPoolFutureInterface.generateBlock().right.value
      resultBlock mustBe Block[String, DummyTransaction](BlockHeader(), Queue(DummyTransaction(5)))
      resultBlock.transactions mustBe Seq(testTransaction)
    }
  }
}
