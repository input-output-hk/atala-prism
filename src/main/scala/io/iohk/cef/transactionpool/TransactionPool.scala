package io.iohk.cef.transactionpool

import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerState, Transaction}
import io.iohk.cef.utils.ByteSizeable

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
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
    val timedQueue: TimedQueue[Tx] = new TimedQueue[Tx](),
    headerGenerator: Seq[Transaction[State]] => Header,
    val maxBlockSize: Int,
    ledgerStateStorage: LedgerStateStorage[State],
    defaultTransactionExpiration: Duration)(implicit blockByteSizeable: ByteSizeable[Block[State, Header, Tx]]) {
  type QueueType = TimedQueue[Tx]
  type BlockType = Block[State, Header, Tx]

  def generateBlock(): BlockType = {
    require(sizeInBytes(TimedQueue()) <= maxBlockSize)
    val blockTxs = getTxs(TimedQueue(), timedQueue, LedgerState(Map()))
    val header = headerGenerator(blockTxs.queue)
    val block = Block(header, blockTxs.queue)
    block
  }

  def processTransaction(transaction: Tx): TransactionPool[State, Header, Tx] =
    copy(queue = timedQueue.enqueue(transaction, defaultTransactionExpiration))

  def removeBlockTransactions(block: BlockType): TransactionPool[State, Header, Tx] = {
    val blockTxs = block.transactions.toSet
    copy(queue = timedQueue.filterNot(blockTxs.contains))
  }

  def copy(
      queue: QueueType = timedQueue,
      headerGenerator: Seq[Transaction[State]] => Header = headerGenerator,
      maxBlockSize: Int = maxBlockSize): TransactionPool[State, Header, Tx] =
    new TransactionPool(queue, headerGenerator, maxBlockSize, ledgerStateStorage, defaultTransactionExpiration)

  def sizeInBytes(txs: QueueType): Int = {
    val header = headerGenerator(txs.queue)
    blockByteSizeable.sizeInBytes(Block(header, txs.queue))
  }

  @tailrec
  private def getTxs(blockTxs: QueueType, remainingQueue: QueueType, ledgerState: LedgerState[State]): QueueType = {
    if (remainingQueue.isEmpty) {
      blockTxs
    } else {
      val (nextTx, tailRemainingQueue) = remainingQueue.dequeue
      val nextBlockTxs = blockTxs.enqueue(nextTx, defaultTransactionExpiration)
      if (sizeInBytes(nextBlockTxs) > maxBlockSize) {
        blockTxs
      } else {
        val newPartitionIds = ledgerStateStorage.slice(nextTx.partitionIds)
        val newLedgerState = mergeLedgerStates(ledgerState, newPartitionIds)
        val block = Block(headerGenerator(nextBlockTxs.queue), nextBlockTxs.queue)
        val applyResult = block(newLedgerState)
        if (applyResult.isLeft) {
          getTxs(blockTxs, tailRemainingQueue, ledgerState)
        } else {
          getTxs(nextBlockTxs, tailRemainingQueue, newLedgerState)
        }
      }
    }
  }

  private def mergeLedgerStates(a: LedgerState[State], b: LedgerState[State]) = {
    new LedgerState[State](a.map ++ b.map)
  }

  implicit private def scalaDurationToJavaDuration(duration: Duration): java.time.Duration =
    java.time.Duration.ofMillis(duration.toMillis)
}
