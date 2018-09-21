package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.storage.dao.MockingLedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerState, Transaction}
import io.iohk.cef.test.{DummyBlockHeader, DummyTransaction, TestClock}
import io.iohk.cef.utils.ByteSizeable
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

import scala.collection.immutable.Queue
import scala.concurrent.duration._

class TransactionPoolSpec
    extends FlatSpec
    with MustMatchers
    with PropertyChecks
    with MockitoSugar
    with MockingLedgerStateStorage[String] {

  behavior of "TransactionPool"

  val emptyHeaderGenerator: Seq[Transaction[String]] => BlockHeader = _ => new BlockHeader {}

  def headerGenerator(size: Int): Seq[Transaction[String]] => BlockHeader = { _ =>
    new DummyBlockHeader(size)
  }

  val sizeableHeaderGenerator: Gen[DummyBlockHeader] = for {
    size <- Gen.posNum[Int]
  } yield DummyBlockHeader(size)

  val sizeableTransactionGenerator: Gen[DummyTransaction] = for {
    size <- Gen.posNum[Int]
  } yield DummyTransaction(size)

  implicit val sizeableHeaderArbitrary = Arbitrary(sizeableHeaderGenerator)
  implicit val sizeableTxArbitrary = Arbitrary(sizeableTransactionGenerator)

  implicit val dummyBlockSerializer: ByteSizeable[Block[String, DummyBlockHeader, DummyTransaction]] =
    new ByteSizeable[Block[String, DummyBlockHeader, DummyTransaction]] {
      override def sizeInBytes(t: Block[String, DummyBlockHeader, DummyTransaction]): Int =
        t.transactions.foldLeft(0)(_ + _.size) + t.header.sizeInBytes
    }

  def totalSize(txs: List[DummyTransaction], header: DummyBlockHeader)(
      implicit txSizeable: ByteSizeable[DummyTransaction],
      headerSizeable: ByteSizeable[DummyBlockHeader]): Int = {
    txs.map(txSizeable.sizeInBytes).foldLeft(0)(_ + _) + headerSizeable.sizeInBytes(header)
  }

  it should "not produce blocks larger than the max block size" in {
    forAll { (txs: Queue[DummyTransaction], header: DummyBlockHeader) =>
      val defaultDuration = 1 minute
      val clock = TestClock()
      val timedQueue = TimedQueue(
        clock,
        txs.map(tx => (tx, clock.instant().plus(java.time.Duration.ofMillis(defaultDuration.toMillis)))))
      val size = totalSize(txs.toList, header)
      val ledgerStateStorage = mockLedgerStateStorage
      when(ledgerStateStorage.slice(ArgumentMatchers.any())).thenReturn(LedgerState[String](Map()))
      val pool =
        new TransactionPool(timedQueue, (_: Seq[Transaction[String]]) => header, size, ledgerStateStorage, 1 minute)
      val block = pool.generateBlock()
      block.header mustBe header
      block.transactions mustBe txs
      val largerPool = pool.processTransaction(DummyTransaction(1))
      val block2 = largerPool.generateBlock()
      block2.header mustBe header
      block2.transactions mustBe txs
    }
  }

  it should "process a transaction" in {
    val header = DummyBlockHeader(1)
    val ledgerStateStorage = mockLedgerStateStorage
    val pool =
      new TransactionPool(TimedQueue(), (_: Seq[Transaction[String]]) => header, 2, ledgerStateStorage, 1 minute)
    val tx = DummyTransaction(2)
    pool.timedQueue.isEmpty mustBe true
    val newPool = pool.processTransaction(tx)
    newPool.timedQueue.size mustBe 1
    newPool.timedQueue.queue.head mustBe tx
  }

  it should "remove block transactions" in {
    val header = DummyBlockHeader(1)
    val ledgerStateStorage = mockLedgerStateStorage
    val pool =
      new TransactionPool(TimedQueue(), (_: Seq[Transaction[String]]) => header, 2, ledgerStateStorage, 1 minute)
    val txs = List(DummyTransaction(2), DummyTransaction(3), DummyTransaction(4))
    pool.timedQueue.isEmpty mustBe true
    val newPool = txs.foldLeft(pool)(_.processTransaction(_))
    newPool.timedQueue.size mustBe 3
    newPool.timedQueue.queue.toList mustBe txs
    val block = Block(header, txs.tail)
    val poolWithoutTxs = newPool.removeBlockTransactions(block)
    poolWithoutTxs.timedQueue.size mustBe 1
    poolWithoutTxs.timedQueue.dequeue._1 mustBe txs.head
  }
}
