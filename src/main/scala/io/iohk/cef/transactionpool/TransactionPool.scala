package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerState, Transaction}
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
class TransactionPool[State, Header <: BlockHeader](
    queue: Queue[Transaction[State]] = Queue(),
    headerGenerator: Seq[Transaction[State]] => Header,
    maxBlockSize: Int)(implicit blockByteSizeable: ByteSizeable[Block[State, Header, Transaction[State]]]) {
  type QueueType = Queue[Transaction[State]]
  type BlockType = Block[State, Header, Transaction[State]]

  val txPoolState = TransactionPoolState(headerGenerator, maxBlockSize)

  def generateBlock(): (TransactionPool[State, Header], BlockType) = {
    val (newQueue, block) = txPoolState(queue)
    (modifyQueue(newQueue), block)
  }

  def processTransaction(transaction: Transaction[State]): TransactionPool[State, Header] =
    modifyQueue(queue.enqueue(transaction))

  def removeBlockTransactions(block: BlockType): TransactionPool[State, Header] ={
    val blockTxs = block.transactions.toSet
    modifyQueue(queue.filterNot(blockTxs.contains))
  }

  private def modifyQueue(newQueue: Queue[Transaction[State]]) =
    new TransactionPool[State, Header](newQueue, headerGenerator, maxBlockSize)
}
