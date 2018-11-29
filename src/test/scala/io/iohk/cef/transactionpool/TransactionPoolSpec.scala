package io.iohk.cef.transactionpool
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerState, Transaction}
import io.iohk.cef.test.{DummyTransaction, TestClock}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}
import org.mockito.ArgumentMatchers._

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.ledger.storage.LedgerStateStorage

class TransactionPoolSpec extends FlatSpec with MustMatchers with PropertyChecks with MockitoSugar {

  behavior of "TransactionPool"

  val mockLedgerStateStorage: LedgerStateStorage[String] = mock[LedgerStateStorage[String]]
  val emptyHeaderGenerator: Seq[Transaction[String]] => BlockHeader = _ => new BlockHeader {}

  def headerGenerator(size: Int): Seq[Transaction[String]] => BlockHeader = _ => BlockHeader()

  val sizeableTransactionGenerator: Gen[DummyTransaction] = for {
    size <- Gen.posNum[Int]
  } yield DummyTransaction(size)

  implicit val sizeableTxArbitrary = Arbitrary(sizeableTransactionGenerator)

  it should "not produce blocks larger than the max block size" in {
    forAll { txs: Queue[DummyTransaction] =>
      val header = BlockHeader()
      val defaultDuration = 1 minute
      val clock = TestClock()
      val timedQueue = TimedQueue(clock, txs.map { tx =>
        tx -> clock.instant().plus(java.time.Duration.ofMillis(defaultDuration.toMillis))
      })
      val ledgerStateStorage = mockLedgerStateStorage
      when(ledgerStateStorage.slice(any())).thenReturn(LedgerState[String]())
      val pool =
        new TransactionPool(timedQueue, (_: Seq[Transaction[String]]) => header, txs.size, ledgerStateStorage, 1 minute)
      val block = pool.generateBlock()
      block.header mustBe header
      block.transactions mustBe txs
      pool.processTransaction(DummyTransaction(1)) match {
        case Left(_) if txs.isEmpty => succeed
        case Left(error) => fail(s"Received message: $error, but expected Right(...)")
        case Right(largerPool) =>
          val block2 = largerPool.generateBlock()
          block2.header mustBe header
          block2.transactions mustBe txs
      }
    }
  }

  it should "process a transaction" in {
    val header = BlockHeader()
    val ledgerStateStorage = mockLedgerStateStorage
    val timedQueue = mock[TimedQueue[DummyTransaction]]
    val oneTxTimedQueue = mock[TimedQueue[DummyTransaction]]
    val twoTxTimedQueue = mock[TimedQueue[DummyTransaction]]
    val defaultExpiration = 1 minute
    val pool =
      new TransactionPool(timedQueue, (_: Seq[Transaction[String]]) => header, 4, ledgerStateStorage, defaultExpiration)
    val tx = DummyTransaction(2)
    when(timedQueue.enqueue(tx, defaultExpiration)).thenReturn(oneTxTimedQueue)
    when(oneTxTimedQueue.enqueue(tx, defaultExpiration)).thenReturn(twoTxTimedQueue)
    val newPool = pool.processTransaction(tx)
    newPool.isRight mustBe true
    verify(timedQueue, times(1)).enqueue(tx, defaultExpiration)
    val tx2 = DummyTransaction(3)
    newPool.map(_.processTransaction(tx2)).isRight mustBe true
    verify(oneTxTimedQueue, times(1)).enqueue(tx2, defaultExpiration)
  }

  it should "remove block transactions" in {
    val header = BlockHeader()
    val ledgerStateStorage = mockLedgerStateStorage
    val timedQueue = mock[TimedQueue[DummyTransaction]]
    val defaultExpiration = 1 minute
    val pool =
      new TransactionPool(
        timedQueue,
        (_: Seq[Transaction[String]]) => header,
        10,
        ledgerStateStorage,
        defaultExpiration)
    val txs = List(DummyTransaction(2), DummyTransaction(3), DummyTransaction(4))
    when(timedQueue.enqueue(any(), ArgumentMatchers.eq(defaultExpiration))).thenReturn(timedQueue)
    when(timedQueue.filterNot(any())).thenReturn(timedQueue)
    val wrappedPool: Either[ApplicationError, TransactionPool[String, DummyTransaction]] = Right(pool)
    val newPoolResult = txs.foldLeft(wrappedPool)((s, e) => s.flatMap(_.processTransaction(e)))
    val block = Block[String, DummyTransaction](header, txs.tail)
    newPoolResult match {
      case Right(newPool) =>
        newPool.removeBlockTransactions(block)
        txs.foreach(tx => verify(timedQueue, times(1)).enqueue(tx, defaultExpiration))
        verify(timedQueue, times(1)).filterNot(any())
      case Left(error) => fail(s"Received message: $error, but expected Right(...)")
    }
  }
}
