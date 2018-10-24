package io.iohk.cef.integration
import io.iohk.cef.LedgerId
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.consensus.raft._
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.test.{DummyBlockHeader, DummyBlockSerializable, DummyTransaction}
import io.iohk.cef.transactionpool.{BlockCreator, TimedQueue, TransactionPoolInterface}
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpecLike, MustMatchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ConsensusPoolItSpec extends FlatSpecLike with MockitoSugar with MustMatchers with Eventually {

  private def mockLedgerStateStorage[State] = mock[LedgerStateStorage[State]]
  type B = Block[String, DummyBlockHeader, DummyTransaction]
  behavior of "ConsensusPoolItSpec"

  it should "push periodical blocks to consensus" in new RealRaftNodeFixture[B] {

    override def clusterIds: Seq[String] = Seq("i1", "i2", "s3")
    val storages = clusterIds.map(_ => new InMemoryPersistentStorage[B](Vector(), 1, ""))
    val Seq(s1, s2, s3) = storages
    implicit val patienceConfig =
      PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))
    implicit val encoders = DummyBlockSerializable.serializable
    val ledgerStateStorage = mockLedgerStateStorage[String]
    val queue = TimedQueue[DummyTransaction]()
    val txPoolFutureInterface =
      new TransactionPoolInterface[String, DummyBlockHeader, DummyTransaction](
        txs => new DummyBlockHeader(txs.size),
        10000,
        ledgerStateStorage,
        1 minute,
        () => queue
      )

    val testExecution = mock[B => Unit]
    val ledgerId: LedgerId = 1
    override def machineCallback: B => Unit = block => {
      testExecution(block)
      txPoolFutureInterface.removeBlockTransactions(block)
    }

    val Seq(t1, t2, t3) = anIntegratedCluster(storages.zip(clusterIds))
    t1.raftNode.electionTimeout().futureValue
    val largeBlockTransactions = (1 to 137).map(DummyTransaction.apply)
    val block2Transactions = (138 to 150).map(DummyTransaction.apply)
    val block3Transactions = (151 to 160).map(DummyTransaction.apply)

    processAllTxs(largeBlockTransactions, txPoolFutureInterface)
    processAllTxs(block2Transactions, txPoolFutureInterface)

    val consensus: Consensus[String, DummyBlockHeader, DummyTransaction] =
      new RaftConsensusInterface(ledgerId, new RaftConsensus(t1.raftNode))

    val blockCreator =
      new BlockCreator[String, DummyBlockHeader, DummyTransaction](
        txPoolFutureInterface,
        consensus,
        0 seconds,
        1 seconds
      )

    val (block1, block2, block3) = (
      Block(DummyBlockHeader(137), largeBlockTransactions),
      Block(DummyBlockHeader(13), block2Transactions),
      Block(DummyBlockHeader(9), block3Transactions)
    )
    val expectedLogEntries = Seq(block1, block2)
    eventually(s1.log.map(_.command) mustBe Seq(expectedLogEntries.head))
    eventually(s1.log.map(_.command) mustBe expectedLogEntries)

    processAllTxs(block3Transactions, txPoolFutureInterface)

    // this proves the queue in transaction pool is empty
    // since we don't have any duplicate transactions
    eventually(s1.log.map(_.command).flatMap(_.transactions).toSet.size mustBe 160)

  }

  private def processAllTxs(
      txs: Seq[DummyTransaction],
      txPoolIf: TransactionPoolInterface[String, DummyBlockHeader, DummyTransaction])(
      implicit executionContext: ExecutionContext
  ) = {
    val result = txs.map(txPoolIf.processTransaction)
    result.foreach(_ mustBe Right(()))
  }
}
