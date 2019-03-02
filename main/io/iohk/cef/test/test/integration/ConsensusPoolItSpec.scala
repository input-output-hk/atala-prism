package io.iohk.cef.integration

import io.iohk.cef.consensus.Consensus
import io.iohk.cef.consensus.raft._
import io.iohk.cef.consensus.raft.testlib.{InMemoryPersistentStorage, RealRaftNodeFixture}
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerId, Transaction}
import io.iohk.cef.test.DummyTransaction
import io.iohk.cef.transactionpool.{BlockCreator, TransactionPoolInterface}
import io.iohk.cef.transactionservice.TransactionChannel
import io.iohk.cef.transactionservice.raft.RaftConsensusInterface
import io.iohk.codecs.nio.auto._
import monix.reactive.MulticastStrategy
import monix.reactive.subjects.ConcurrentSubject
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpecLike, MustMatchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ConsensusPoolItSpec extends FlatSpecLike with MockitoSugar with MustMatchers with Eventually {

  private def mockLedgerStateStorage = mock[LedgerStateStorage[String]]
  type B = Block[String, DummyTransaction]
  behavior of "ConsensusPoolItSpec"

  it should "push periodical blocks to consensus" in new RealRaftNodeFixture[B] {

    override def clusterIds: Seq[String] = Seq("i1", "i2", "s3")
    val storages = clusterIds.map(_ => new InMemoryPersistentStorage[B](Vector(), 1, ""))
    val Seq(s1, s2, s3) = storages
    implicit val patienceConfig =
      PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(200, Millis)))
    val ledgerStateStorage = mockLedgerStateStorage
    val generateHeader: Seq[Transaction[String]] => BlockHeader = _ => BlockHeader()

    val transactionChannel =
      ConcurrentSubject[DummyTransaction](MulticastStrategy.publish)(monix.execution.Scheduler.global)
    val txPoolFutureInterface =
      TransactionPoolInterface[String, DummyTransaction](
        generateHeader,
        maxBlockSize = 3,
        ledgerStateStorage,
        1 minute,
        transactionChannel
      )

    val testExecution = mock[B => Unit]
    val ledgerId: LedgerId = "1"
    override def machineCallback: B => Unit = block => {
      testExecution(block)
      txPoolFutureInterface.removeBlockTransactions(block)
    }

    val Seq(t1, t2, t3) = anIntegratedCluster(storages.zip(clusterIds))
    t1.raftNode.electionTimeout().futureValue
    val block1Transactions = (1 to 3).map(DummyTransaction)
    val block2Transactions = (4 to 5).map(DummyTransaction)
    val block3Transactions = (6 to 7).map(DummyTransaction)

    processAllTxs(block1Transactions, transactionChannel)
    processAllTxs(block2Transactions, transactionChannel)

    val consensus: Consensus[String, DummyTransaction] =
      new RaftConsensusInterface(ledgerId, new RaftConsensus(t1.raftNode))

    import monix.execution.Scheduler.Implicits.global

    val newBlockChannel = ConcurrentSubject[Block[String, DummyTransaction]](MulticastStrategy.publish)

    val blockCreator =
      new BlockCreator[String, DummyTransaction](
        txPoolFutureInterface,
        consensus,
        newBlockChannel,
        0 seconds,
        1 seconds
      )

    val (block1, block2, block3) = (
      Block[String, DummyTransaction](BlockHeader(), block1Transactions),
      Block[String, DummyTransaction](BlockHeader(), block2Transactions),
      Block[String, DummyTransaction](BlockHeader(), block3Transactions)
    )

    eventually {
      val existingTransactions = s1.log.map(_.command).flatMap(_.transactions).toSet
      val expectedTransactions = List(block1, block2).flatMap(_.transactions).toSet
      existingTransactions mustBe expectedTransactions
    }

    processAllTxs(block3Transactions, transactionChannel)

    // this proves the queue in transaction pool is empty
    // since we don't have any duplicate transactions
    eventually(
      s1.log.map(_.command).flatMap(_.transactions).toSet.size mustBe
        block1Transactions.size + block2Transactions.size + block3Transactions.size
    )
  }

  private def processAllTxs(txs: Seq[DummyTransaction], transactionChannel: TransactionChannel[DummyTransaction])(
      implicit executionContext: ExecutionContext
  ) = {
    txs.foreach(transactionChannel.onNext)
  }
}
