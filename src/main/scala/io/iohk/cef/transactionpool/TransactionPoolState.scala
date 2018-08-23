package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.{Block, BlockHeader, StateM, Transaction}

import scala.collection.immutable.Queue

object TransactionPoolState {
  def apply[State, Header <: BlockHeader](
             headerGenerator: Seq[Transaction[State]] => Header,
             maxTxsPerBlock: Int): StateM[Queue[Transaction[State]], Block[State, Header, Transaction[State]]] = {
    StateM(s => {
      val (blockTxs, tail) = s.splitAt(maxTxsPerBlock)
      (tail, Block(headerGenerator(blockTxs), blockTxs))
    })
  }
}
