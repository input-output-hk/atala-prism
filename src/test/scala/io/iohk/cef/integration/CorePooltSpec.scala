package io.iohk.cef.integration
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.core.{Envelope, Everyone, NodeCore}
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.network.{MessageStream, Network, NodeId}
import io.iohk.cef.test.{DummyBlockHeader, DummyBlockSerializable, DummyTransaction}
import io.iohk.cef.transactionpool.{TimedQueue, TransactionPoolActorModelInterface, TransactionPoolFutureInterface}
import io.iohk.cef.utils.ByteSizeable
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, _}

class CorePooltSpec
    extends TestKit(ActorSystem("CorePoolIntegration"))
    with FlatSpecLike
    with MustMatchers
    with BeforeAndAfterAll
    with MockitoSugar {

  private def mockLedgerStateStorage[State] = mock[LedgerStateStorage[State]]

  import io.iohk.cef.ledger.ByteSizeableImplicits._
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  class TestableTransactionPoolActorModelInterface[State, Header <: BlockHeader, Tx <: Transaction[State]](
      headerGenerator: Seq[Transaction[State]] => Header,
      maxTxSizeInBytes: Int,
      ledgerStateStorage: LedgerStateStorage[State],
      defaultDurationTxs: Duration,
      timedQueue: TimedQueue[Tx])(implicit blockByteSizeable: ByteSizeable[Block[State, Header, Tx]])
      extends TransactionPoolActorModelInterface[State, Header, Tx](
        system.actorOf,
        headerGenerator,
        maxTxSizeInBytes,
        ledgerStateStorage,
        defaultDurationTxs,
        () => timedQueue) {

    lazy val testActorRef = TestActorRef[TransactionPoolActor](Props(new TransactionPoolActor()))
    override def poolActor: ActorRef = testActorRef
  }

  behavior of "CorePoolItSpec"

  it should "process a transaction" in {
    implicit val timeout = Timeout(10 seconds)
    implicit val executionContext = ExecutionContext.global
    import DummyTransaction._
    val ledgerStateStorage = mockLedgerStateStorage[String]
    val queue = TimedQueue[DummyTransaction]()
    val txPoolActorModelInterface =
      new TestableTransactionPoolActorModelInterface[String, DummyBlockHeader, DummyTransaction](
        txs => new DummyBlockHeader(txs.size),
        10,
        ledgerStateStorage,
        1 minute,
        queue
      )
    val txPoolFutureInterface =
      new TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction](txPoolActorModelInterface)
    val consensus = mock[Consensus[String, DummyBlockHeader, DummyTransaction]]
    val txNetwork = mock[Network[Envelope[DummyTransaction]]]
    val blockNetwork = mock[Network[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]]
    val consensusMap = Map(1 -> (txPoolFutureInterface, consensus))
    val me = NodeId("3112")
    val mockTxMessageStream = mock[MessageStream[Envelope[DummyTransaction]]]
    val mockBlockMessageStream =
      mock[MessageStream[Envelope[Block[String, DummyBlockHeader, DummyTransaction]]]]
    when(txNetwork.messageStream).thenReturn(mockTxMessageStream)
    when(blockNetwork.messageStream).thenReturn(mockBlockMessageStream)
    when(mockTxMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    when(mockBlockMessageStream.foreach(ArgumentMatchers.any())).thenReturn(Future.successful(()))
    val core = new NodeCore(consensusMap, txNetwork, blockNetwork, me)(
      Envelope.envelopeSerializer(DummyTransaction.serializable),
      Envelope.envelopeSerializer(DummyBlockSerializable.serializable),
      executionContext
    )
    val testTransaction = DummyTransaction(5)
    val envelope = Envelope(testTransaction, 1, Everyone)
    val result = Await.result(core.receiveTransaction(envelope), 10 seconds)
    result mustBe Right(())
    val pool = txPoolActorModelInterface.testActorRef.underlyingActor.pool
    queue.size mustBe 1
    val (tx, timedQueue) = queue.dequeue
    tx mustBe testTransaction
    timedQueue.isEmpty mustBe true
  }
}
