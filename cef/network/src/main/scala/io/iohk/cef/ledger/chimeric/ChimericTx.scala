package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.{LedgerError, LedgerState, Transaction}

class ChimericTx(fragments: Seq[ChimericTxFragment]) extends Transaction[ChimericLedgerState] {
  override def apply(s: LedgerState[ChimericLedgerState]): Either[LedgerError, LedgerState[ChimericLedgerState]] = {
  }

  override def partitionIds: Set[String] = fragments.foldLeft(Set[String]())((st, curr) => st ++ curr.partitionIds)
}
