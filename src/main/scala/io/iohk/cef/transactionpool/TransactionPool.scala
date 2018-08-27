package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.utils.ByteSizeable

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
    val queue: Queue[Tx with Transaction[State]] = Queue(),
    headerGenerator: Seq[Tx] => Header,
    val maxBlockSize: Int)(implicit blockByteSizeable: ByteSizeable[Block[State, Header, Tx]]) {
  type QueueType = Queue[Tx]
  type BlockType = Block[State, Header, Tx]

  val txPoolState =
    TransactionPoolState(headerGenerator, maxBlockSize, blockByteSizeable)

  def generateBlock(): (TransactionPool[State, Header, Tx], BlockType) = {
    val (newQueue, block) = txPoolState(queue)
    (copy(queue = newQueue), block)
  }

  def processTransaction(transaction: Tx): TransactionPool[State, Header, Tx] =
    copy(queue = queue.enqueue(transaction))

  def removeBlockTransactions(block: BlockType): TransactionPool[State, Header, Tx] ={
    val blockTxs = block.transactions.toSet
    copy(queue = queue.filterNot(blockTxs.contains))
  }

  def copy(queue: Queue[Tx with Transaction[State]] = queue,
           headerGenerator: Seq[Tx] => Header = headerGenerator,
           maxBlockSize: Int = maxBlockSize): TransactionPool[State, Header, Tx] =
    new TransactionPool(queue, headerGenerator, maxBlockSize)
}
