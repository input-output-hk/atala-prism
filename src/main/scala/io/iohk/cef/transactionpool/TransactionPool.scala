package io.iohk.cef.transactionpool

import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.annotation.tailrec
import scala.concurrent.duration._

/**
  * A TransactionPool serves as a queue for transactions before being added to a blockchain.
  * Besides holding the transactions, this class also has the responsibility to create blocks.
  * @param timedQueue The starting queue for this pool
  * @param headerGenerator generates a block header based on a list of transactions
  * @param maxBlockSize maximum block size in bytes
  * @tparam State the ledger state type
  */
class TransactionPool[State, Tx <: Transaction[State]](
    timedQueue: TimedQueue[Tx] = new TimedQueue[Tx](),
    headerGenerator: Seq[Transaction[State]] => BlockHeader,
    maxBlockSize: Int,
    ledgerStateStorage: LedgerStateStorage[State],
    defaultTransactionExpiration: Duration) {
  require(0 <= maxBlockSize)
  type Q = TimedQueue[Tx]
  type B = Block[State, Tx]

  /**
    * Generates a block out of this pool's transactions.
    * @return Some block if the pool contains
    */
  def generateBlock(): B = {

    val blockTxs = getTxs(TimedQueue(), timedQueue)
    val header = headerGenerator(blockTxs.queue)
    val block = Block[State, Tx](header, blockTxs.queue)
    block
  }

  def size: Int = timedQueue.size

  def processTransaction(transaction: Tx): Either[TransactionPoolError, TransactionPool[State, Tx]] = {
    if (size(Seq(transaction)) > maxBlockSize) {
      Left(TransactionLargerThanMaxBlockSize(transaction, maxBlockSize))
    } else {
      Right(copy(queue = timedQueue.enqueue(transaction, defaultTransactionExpiration)))
    }
  }

  def removeBlockTransactions(block: B): TransactionPool[State, Tx] = {
    val blockTxs = block.transactions.toSet
    copy(queue = timedQueue.filterNot(blockTxs.contains))
  }

  private def copy(
      queue: Q = timedQueue,
      headerGenerator: Seq[Transaction[State]] => BlockHeader = headerGenerator,
      maxBlockSize: Int = maxBlockSize): TransactionPool[State, Tx] =
    new TransactionPool(queue, headerGenerator, maxBlockSize, ledgerStateStorage, defaultTransactionExpiration)

  private def size(txs: Q): Int = txs.queue.size

  private def size(txs: Seq[Tx]): Int = txs.size

  @tailrec
  private def getTxs(blockTxs: Q, remainingQueue: Q): Q = {
    if (remainingQueue.isEmpty) {
      blockTxs
    } else {
      val (nextTx, tailRemainingQueue) = remainingQueue.dequeue
      val nextBlockTxs = blockTxs.enqueue(nextTx, defaultTransactionExpiration)
      if (size(nextBlockTxs) > maxBlockSize) {
        blockTxs
      } else {
        val block = Block[State, Tx](headerGenerator(nextBlockTxs.queue), nextBlockTxs.queue)
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

}
