package io.iohk.cef.integration
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.consensus.raft.{InMemoryPersistentStorage, RaftConsensusInterface, RealRaftNode}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.test.{DummyBlockHeader, DummyBlockSerializable, DummyTransaction}
import io.iohk.cef.transactionpool.{BlockCreator, TimedQueue, TransactionPoolFutureInterface}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpecLike, MustMatchers}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ConsensusPoolItSpec extends TestKit(ActorSystem("testActorModel"))
  with ImplicitSender
  with FlatSpecLike
  with MockitoSugar
  with TxPoolFixture
  with MustMatchers {

  private def mockLedgerStateStorage[State] = mock[LedgerStateStorage[State]]
  type B = Block[String, DummyBlockHeader, DummyTransaction]
  behavior of "ConsensusPoolItSpec"

  it should "push periodical blocks to consensus" in new RealRaftNode[B] {

    override def clusterIds: Seq[String] = Seq("i1", "i2", "s3")
    val storages = clusterIds.map(_ => new InMemoryPersistentStorage[B](Vector(), 1, ""))
    val Seq(s1, s2, s3) = storages
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

    val testExecution = mock[B => Unit]
    val ledgerId: LedgerId = 1
    override def machineCallback: B => Unit = block => {
      testExecution(block)
      txPoolFutureInterface.removeBlockTxs(block).futureValue
    }

    val Seq(t1, t2, t3) = anIntegratedCluster(storages.zip(clusterIds))
    t1.raftNode.electionTimeout().futureValue
    val largeBlockTransactions = (1 to 137).map(DummyTransaction.apply)
    val block2Transactions = (138 to 150).map(DummyTransaction.apply)
    val block3Transactions = (151 to 160).map(DummyTransaction.apply)
    processAllTxs(largeBlockTransactions, txPoolFutureInterface)
    processAllTxs(block2Transactions, txPoolFutureInterface)

    val consensus: Consensus[String, DummyBlockHeader, DummyTransaction] =
      new RaftConsensusInterface(ledgerId, t1.raftNode)

    val blockCreator =
      system.actorOf(Props(new BlockCreator[String, DummyBlockHeader, DummyTransaction](
        txPoolActorModelInterface,
        consensus,
        100 minutes,
        100 minutes
      )))
    val (block1, block2, block3) = (
      Block(DummyBlockHeader(137), largeBlockTransactions),
      Block(DummyBlockHeader(13), block2Transactions),
      Block(DummyBlockHeader(9), block3Transactions)
    )

    val expectedLogEntries = Seq(block1, block2)

    txPoolActorModelInterface.testActorRef.underlyingActor.pool.size mustBe 150
    blockCreator ! BlockCreator.Execute(Some(implicitly[ActorRef]))
    expectMsg(30 seconds, Right[ApplicationError, Unit](()))
    txPoolActorModelInterface.testActorRef.underlyingActor.pool.size mustBe block2Transactions.size
    s1.log.map(_.command) mustBe Seq(expectedLogEntries.head)
    val testProbe = TestProbe()

    blockCreator ! BlockCreator.Execute(Some(testProbe.ref))
    testProbe.expectMsg(30 seconds, Right[ApplicationError, Unit](()))
    s1.log.map(_.command) mustBe expectedLogEntries
    val inOrderExecution = Mockito.inOrder(testExecution)
    //one call per cluster node
    inOrderExecution.verify(testExecution, times(3)).apply(block1)
    //Raft is applying the block but it is not calling the callback.
    inOrderExecution.verify(testExecution, times(3)).apply(block2)
    txPoolActorModelInterface.testActorRef.underlyingActor.pool.size mustBe 0
  }

  private def processAllTxs(txs: Seq[DummyTransaction],
                            txPoolIf: TransactionPoolFutureInterface[String, DummyBlockHeader, DummyTransaction])(
      implicit executionContext: ExecutionContext
  ) = {
    val result = Future.sequence(txs.map(txPoolIf.processTransaction))
    result.futureValue.foreach(_ mustBe Right(()))
  }
}
