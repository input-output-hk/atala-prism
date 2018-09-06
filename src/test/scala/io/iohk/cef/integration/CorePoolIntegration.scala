package io.iohk.cef.integration
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.core.{Envelope, NodeCore}
import io.iohk.cef.network.{NetworkComponent, NodeId}
import io.iohk.cef.test.{DummyBlockHeader, DummyTransaction}
import io.iohk.cef.transactionpool.{TransactionPoolActorModelInterface, TransactionPoolFutureInterface}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent._

class CorePoolIntegration extends TestKit(ActorSystem("CorePoolIntegration")) with FlatSpecLike with MustMatchers with BeforeAndAfterAll with MockitoSugar {

  import io.iohk.cef.ledger.ByteSizeableImplicits._
  import io.iohk.cef.ledger.ByteStringSerializableImplicits._

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  behavior of "NetworkPoolIntegration"

  it should "process a transaction" in {
    implicit val timeout = Timeout(10 seconds)
    implicit val executionContext = ExecutionContext.global
    val txPoolActorModelInterface = new TransactionPoolActorModelInterface[String, DummyBlockHeader, DummyTransaction](system.actorOf, txs => new DummyBlockHeader(txs.size), 10) {
      val testActorRef = TestActorRef[TransactionPoolActor]
      override val poolActor: ActorRef = testActorRef
    }
    val txPoolFutureInterface = new TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction](txPoolActorModelInterface)
    val consensus = mock[Consensus[String, DummyTransaction]]
    val networkComponent = mock[NetworkComponent[String]]
    val consensusMap = Map(1 -> (txPoolFutureInterface, consensus))
    val me = NodeId("Me")
    val core = new NodeCore(consensusMap, networkComponent, me)
    val testTransaction = DummyTransaction(5)
    val result = Await.result(core.receiveTransaction(Envelope(testTransaction, 1, _ => true)), 10 seconds)
    result mustBe Right(())
    val pool = txPoolActorModelInterface.testActorRef.underlyingActor.pool
    pool.queue.size mustBe 1
    pool.queue.dequeue mustBe (testTransaction, Queue())
  }
}
