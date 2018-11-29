package io.iohk.cef.ledger.storage

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.{Block, Transaction}

abstract class LedgerStorage[S, Tx <: Transaction[S]](ledgerId: LedgerId) {
  def push(block: Block[S, Tx]): Unit
}
