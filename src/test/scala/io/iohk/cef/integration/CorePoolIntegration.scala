package io.iohk.cef.integration
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.core.{Envelope, NodeCore}
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.network.{NetworkComponent, NodeId}
import io.iohk.cef.test.{DummyBlockHeader, DummyBlockSerializable, DummyTransaction}
import io.iohk.cef.transactionpool.{TransactionPoolActorModelInterface, TransactionPoolFutureInterface}
import io.iohk.cef.utils.ByteSizeable
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, _}

class CorePoolIntegration
    extends TestKit(ActorSystem("CorePoolIntegration"))
    with FlatSpecLike
    with MustMatchers
    with BeforeAndAfterAll
    with MockitoSugar {

  import io.iohk.cef.ledger.ByteSizeableImplicits._
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  class TestableTransactionPoolActorModelInterface[State, Header <: BlockHeader, Tx <: Transaction[State]](
      headerGenerator: Seq[Transaction[State]] => Header,
      maxTxSizeInBytes: Int)(implicit blockByteSizeable: ByteSizeable[Block[State, Header, Tx]])
      extends TransactionPoolActorModelInterface[State, Header, Tx](system.actorOf, headerGenerator, maxTxSizeInBytes) {

    lazy val testActorRef = TestActorRef[TransactionPoolActor](Props(new TransactionPoolActor()))
    override def poolActor: ActorRef = testActorRef
  }

  behavior of "NetworkPoolIntegration"

  it should "process a transaction" in {
    implicit val timeout = Timeout(10 seconds)
    implicit val executionContext = ExecutionContext.global
    import DummyTransaction._
    val txPoolActorModelInterface =
      new TestableTransactionPoolActorModelInterface[String, DummyBlockHeader, DummyTransaction](
        txs => new DummyBlockHeader(txs.size),
        10)
    val txPoolFutureInterface =
      new TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction](txPoolActorModelInterface)
    val consensus = mock[Consensus[String, DummyTransaction]]
    val networkComponent = mock[NetworkComponent[String]]
    val consensusMap = Map(1 -> (txPoolFutureInterface, consensus))
    val me = NodeId("3112")
    val core = new NodeCore(consensusMap, networkComponent, me)(
      DummyTransaction.serializable,
      DummyBlockSerializable.serializable,
      executionContext)
    val testTransaction = DummyTransaction(5)
    val envelope = Envelope(testTransaction, 1, _ => true)
    when(networkComponent.disseminate(envelope)).thenReturn(Future.successful(Right(())))
    val result = Await.result(core.receiveTransaction(envelope), 10 seconds)
    result mustBe Right(())
    val pool = txPoolActorModelInterface.testActorRef.underlyingActor.pool
    pool.queue.size mustBe 1
    pool.queue.dequeue mustBe (testTransaction, Queue())
  }
}
