package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.{Block, BlockHeader, StateM, Transaction}
import io.iohk.cef.utils.ByteSizeable

import scala.annotation.tailrec
import scala.collection.immutable.Queue

object TransactionPoolState {
  def apply[State, Header <: BlockHeader](
             headerGenerator: Seq[Transaction[State]] => Header,
             maxBlockSize: Int)(
      implicit blockByteSizeable: ByteSizeable[Block[State, Header, Transaction[State]]]
  ): StateM[Queue[Transaction[State]], Block[State, Header, Transaction[State]]] = {

    def sizeInBytes(txs: Queue[Transaction[State]]): Int =
      blockByteSizeable.sizeInBytes(Block(headerGenerator(txs), txs))

    @tailrec
    def getTxs(blockTxs: Queue[Transaction[State]], remainingQueue: Queue[Transaction[State]]): (Queue[Transaction[State]], Queue[Transaction[State]]) = {
      if(remainingQueue.isEmpty) {
        (blockTxs, remainingQueue)
      } else {
        val (nextTx, tailRemainingQueue) = remainingQueue.dequeue
        val nextBlockTxs = blockTxs.enqueue(nextTx)
        if(sizeInBytes(nextBlockTxs) >= maxBlockSize) {
          (blockTxs, remainingQueue)
        } else {
          getTxs(nextBlockTxs, tailRemainingQueue)
        }
      }
    }

    StateM(s => {
      val (blockTxs, tail) = getTxs(Queue(), s)
      (tail, Block(headerGenerator(blockTxs), blockTxs))
    })
  }
}
