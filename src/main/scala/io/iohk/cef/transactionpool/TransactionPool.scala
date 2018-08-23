package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.utils.ByteSizeable

import scala.collection.immutable.Queue

class TransactionPool[State, Header <: BlockHeader](
    queue: Queue[Transaction[State]] = Queue(),
    headerGenerator: Seq[Transaction[State]] => Header,
    maxBlockSize: Int)(implicit blockByteSizeable: ByteSizeable[Block[State, Header, Transaction[State]]]) {
  type QueueType = Queue[Transaction[State]]
  type BlockType = Block[State, Header, Transaction[State]]

  val state = TransactionPoolState(headerGenerator, maxBlockSize)

  def generateBlock(): (TransactionPool[State, Header], BlockType) = {
    val (newQueue, block) = state(queue)
    (new TransactionPool(newQueue, headerGenerator, maxBlockSize), block)
  }

  def processTransaction(transaction: Transaction[State]): TransactionPool[State, Header] =
    new TransactionPool(queue.enqueue(transaction), headerGenerator, maxBlockSize)

  def removeBlockTransactions(block: BlockType): TransactionPool[State, Header] ={
    val blockTxs = block.transactions.toSet
    new TransactionPool(queue.filterNot(blockTxs.contains), headerGenerator, maxBlockSize)
  }
}
