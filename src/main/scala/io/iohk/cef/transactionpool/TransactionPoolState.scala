package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.{Block, BlockHeader, StateM, Transaction}
import io.iohk.cef.utils.ByteSizeable

import scala.annotation.tailrec
import scala.collection.immutable.Queue

object TransactionPoolState {
  def apply[State, Header <: BlockHeader, Tx <: Transaction[State]](
             headerGenerator: Seq[Tx] => Header,
             maxBlockSize: Int,
             blockByteSizeable: ByteSizeable[Block[State, Header, Tx]]
  ): StateM[Queue[Tx], Block[State, Header, Tx]] = {

    def sizeInBytes(txs: Queue[Tx]): Int = {
      val header = headerGenerator(txs)
      blockByteSizeable.sizeInBytes(Block(header, txs))
    }

    @tailrec
    def getTxs(blockTxs: Queue[Tx], remainingQueue: Queue[Tx]): (Queue[Tx], Queue[Tx]) = {
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

    StateM(s => {
      require(sizeInBytes(Queue()) <= maxBlockSize)
      val (blockTxs, tail) = getTxs(Queue(), s)
      (tail, Block(headerGenerator(blockTxs), blockTxs))
    })
  }
}
