package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.{Block, Transaction}

import scala.collection.immutable.Queue

case class TransactionPool[State, Header](
    queue: Queue[Transaction[State]] = Queue(),
    headerGenerator: Seq[Transaction[State]] => Header,
    maxTxsPerBlock: Int) {
  type QueueType = Queue[Transaction[State]]
  type BlockType = Block[State, Header, Transaction[State]]

  val state = TransactionPoolState(headerGenerator, maxTxsPerBlock)

  def generateBlock(): (TransactionPool[State, Header], BlockType) = {
    val (newQueue, block) = state(queue)
    (copy(queue = newQueue), block)
  }

  def processTransaction(transaction: Transaction[State]): TransactionPool[State, Header] =
    copy(queue = queue.enqueue(transaction))

  def removeBlockTransactions(block: BlockType): TransactionPool[State, Header] ={
    val blockTxs = block.transactions.toSet
    copy(queue = queue.filterNot(blockTxs.contains))
  }

  def modifyQueue(modification: QueueType => QueueType): TransactionPool[State, Header] = {
    copy(queue = modification(queue))
  }
}
