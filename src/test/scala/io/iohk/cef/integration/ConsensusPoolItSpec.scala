package io.iohk.cef.integration
import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import io.iohk.cef.consensus.raft.{InMemoryPersistentStorage, RealRaftNode}
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.test.{DummyBlockHeader, DummyBlockSerializable, DummyTransaction}
import io.iohk.cef.transactionpool.{TimedQueue, TransactionPoolFutureInterface}
import org.scalatest.{FlatSpecLike, MustMatchers}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class ConsensusPoolItSpec extends TestKit(ActorSystem("testActorModel"))
  with FlatSpecLike
  with MockitoSugar
  with TxPoolFixture
  with MustMatchers {

  private def mockLedgerStateStorage[State] = mock[LedgerStateStorage[State]]
  type B = Block[String, DummyBlockHeader, DummyTransaction]
  behavior of "ConsensusPoolItSpec"

  it should "push periodical blocks to consensus" in new RealRaftNode[B] {

    override def clusterIds: Seq[String] = Seq("i1", "i2", "i3")
    implicit val futureTimeout: Timeout = 30 seconds
    implicit val executionContext: ExecutionContext = system.dispatcher
    implicit val encoders = DummyBlockSerializable.serializable
    val ledgerStateStorage = mockLedgerStateStorage[String]
    val queue = TimedQueue[DummyTransaction]()
    val txPoolActorModelInterface =
      new TestableTransactionPoolActorModelInterface[String, DummyBlockHeader, DummyTransaction](
        txs => new DummyBlockHeader(txs.size),
        10000,
        ledgerStateStorage,
        1 minute,
        queue
      )
    val txPoolFutureInterface =
      new TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction](txPoolActorModelInterface)

    override def machineCallback: B => Unit = block => {
      txPoolFutureInterface.removeBlockTxs(block).futureValue
    }
    val storages = clusterIds.map(_ => new InMemoryPersistentStorage[B](Vector(), 1, ""))
    val Seq(s1, s2, s3) = storages

    val Seq(t1, t2, t3) = anIntegratedCluster(storages.zip(clusterIds))
    t1.raftNode.electionTimeout().futureValue
    val largeBlockTransactions = (1 to 141).map(DummyTransaction.apply)
    val block2Transactions = (142 to 150).map(DummyTransaction.apply)
    val block3Transactions = (151 to 160).map(DummyTransaction.apply)
    processAllTxs(largeBlockTransactions, txPoolFutureInterface)
    processAllTxs(block2Transactions, txPoolFutureInterface)
    
  }

  private def processAllTxs(txs: Seq[DummyTransaction],
                            txPoolIf: TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction])(
      implicit executionContext: ExecutionContext
  ) = {
    val result = Future.sequence(txs.map(txPoolIf.processTransaction))
    result.futureValue.foreach(_ mustBe Right(()))
  }

}
