package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.{LedgerError, Partitioned, Transaction}

class ChimericTx(fragments: Seq[ChimericTxFragment]) extends Transaction[ChimericLedgerState] {
  override def apply(s: Partitioned[ChimericLedgerState]): Either[LedgerError, Partitioned[ChimericLedgerState]] = {

  }

  override def partitionIds: Set[String] = ???
}

