package io.iohk.cef.transactionpool

import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.utils.ByteSizeable

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.language.implicitConversions

/**
  * A TransactionPool serves as a queue for transactions before being added to a blockchain.
  * Besides holding the transactions, this class also has the responsibility to create blocks.
  * @param timedQueue The starting queue for this pool
  * @param headerGenerator generates a block header based on a list of transactions
  * @param maxBlockSize maximum block size in bytes
  * @param blockByteSizeable type class that allows for any block to be measured in bytes
  * @tparam State the ledger state type
  * @tparam Header the block header type
  */
class TransactionPool[State, Header <: BlockHeader, Tx <: Transaction[State]](
    timedQueue: TimedQueue[Tx] = new TimedQueue[Tx](),
    headerGenerator: Seq[Transaction[State]] => Header,
    maxBlockSize: Int,
    ledgerStateStorage: LedgerStateStorage[State],
    defaultTransactionExpiration: Duration)(implicit blockByteSizeable: ByteSizeable[Block[State, Header, Tx]]) {
  require(sizeInBytes(Seq()) <= maxBlockSize)
  type Q = TimedQueue[Tx]
  type B = Block[State, Header, Tx]

  /**
    * Generates a block out of this pool's transactions.
    * @return Some block if the pool contains
    */
  def generateBlock(): B = {
    val blockTxs = getTxs(TimedQueue(), timedQueue)
    val header = headerGenerator(blockTxs.queue)
    val block = Block(header, blockTxs.queue)
    block
  }

  def size: Int = timedQueue.size

  def processTransaction(transaction: Tx): Either[TransactionPoolError, TransactionPool[State, Header, Tx]] = {
    if (sizeInBytes(Seq(transaction)) > maxBlockSize) {
      Left(TransactionLargerThanMaxBlockSize(transaction, maxBlockSize))
    } else {
      Right(copy(queue = timedQueue.enqueue(transaction, defaultTransactionExpiration)))
    }
  }

  def removeBlockTransactions(block: B): TransactionPool[State, Header, Tx] = {
    val blockTxs = block.transactions.toSet
    copy(queue = timedQueue.filterNot(blockTxs.contains))
  }

  private def copy(
      queue: Q = timedQueue,
      headerGenerator: Seq[Transaction[State]] => Header = headerGenerator,
      maxBlockSize: Int = maxBlockSize): TransactionPool[State, Header, Tx] =
    new TransactionPool(queue, headerGenerator, maxBlockSize, ledgerStateStorage, defaultTransactionExpiration)

  private def sizeInBytes(txs: Q): Int = sizeInBytes(txs.queue)

  private def sizeInBytes(txs: Seq[Tx]): Int = {
    val header = headerGenerator(txs)
    blockByteSizeable.sizeInBytes(Block(header, txs))
  }

  @tailrec
  private def getTxs(blockTxs: Q, remainingQueue: Q): Q = {
    if (remainingQueue.isEmpty) {
      blockTxs
    } else {
      val (nextTx, tailRemainingQueue) = remainingQueue.dequeue
      val nextBlockTxs = blockTxs.enqueue(nextTx, defaultTransactionExpiration)
      if (sizeInBytes(nextBlockTxs) > maxBlockSize) {
        blockTxs
      } else {
        val block = Block(headerGenerator(nextBlockTxs.queue), nextBlockTxs.queue)
        //Do we need to optimize this?
        val newLedgerState = ledgerStateStorage.slice(block.partitionIds)
        val applyResult = block(newLedgerState)
        if (applyResult.isLeft) {
          getTxs(blockTxs, tailRemainingQueue)
        } else {
          getTxs(nextBlockTxs, tailRemainingQueue)
        }
      }
    }
  }

  implicit private def scalaDurationToJavaDuration(duration: Duration): java.time.Duration =
    java.time.Duration.ofMillis(duration.toMillis)
}
