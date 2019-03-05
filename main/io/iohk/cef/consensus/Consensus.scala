package io.iohk.cef.consensus

import io.iohk.cef.ledger.{LedgerId, Transaction}

trait Consensus[State, Tx <: Transaction[State]] {

  def ledgerId: LedgerId

}
