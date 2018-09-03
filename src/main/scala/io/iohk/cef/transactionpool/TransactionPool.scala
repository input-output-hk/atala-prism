package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.utils.ByteSizeable

import scala.annotation.tailrec
import scala.collection.immutable.Queue

/**
  * A TransactionPool serves as a queue for transactions before being added to a blockchain.
  * Besides holding the transactions, this class also has the responsibility to create blocks.
  * @param queue The starting queue for this pool
  * @param headerGenerator generates a block header based on a list of transactions
  * @param maxBlockSize maximum block size in bytes
  * @param blockByteSizeable type class that allows for any block to be measured in bytes
  * @tparam State the ledger state type
  * @tparam Header the block header type
  */
class TransactionPool[State, Header <: BlockHeader, Tx <: Transaction[State]](
    val queue: Queue[Tx] = Queue(),
    headerGenerator: Seq[Transaction[State]] => Header,
    val maxBlockSize: Int)(implicit blockByteSizeable: ByteSizeable[Block[State, Header, Tx]]) {
  type QueueType = Queue[Tx]
  type BlockType = Block[State, Header, Tx]

  def generateBlock(): (TransactionPool[State, Header, Tx], BlockType) = {
    require(sizeInBytes(Queue()) <= maxBlockSize)
    val (blockTxs, tail) = getTxs(Queue(), queue)
    val header = headerGenerator(blockTxs)
    (copy(queue = tail), Block(header, blockTxs))
  }

  def processTransaction(transaction: Tx): TransactionPool[State, Header, Tx] =
    copy(queue = queue.enqueue(transaction))

  def removeBlockTransactions(block: BlockType): TransactionPool[State, Header, Tx] ={
    val blockTxs = block.transactions.toSet
    copy(queue = queue.filterNot(blockTxs.contains))
  }

  def copy(queue: QueueType = queue,
           headerGenerator: Seq[Transaction[State]] => Header = headerGenerator,
           maxBlockSize: Int = maxBlockSize): TransactionPool[State, Header, Tx] =
    new TransactionPool(queue, headerGenerator, maxBlockSize)


  def sizeInBytes(txs: QueueType): Int = {
    val header = headerGenerator(txs)
    blockByteSizeable.sizeInBytes(Block(header, txs))
  }

  @tailrec
  private def getTxs(blockTxs: QueueType, remainingQueue: QueueType): (QueueType, QueueType) = {
    if(remainingQueue.isEmpty) {
      (blockTxs, remainingQueue)
    } else {
      val (nextTx, tailRemainingQueue) = remainingQueue.dequeue
      val nextBlockTxs = blockTxs.enqueue(nextTx)
      if(sizeInBytes(nextBlockTxs) > maxBlockSize) {
        (blockTxs, remainingQueue)
      } else {
        getTxs(nextBlockTxs, tailRemainingQueue)
      }
    }
  }
}
