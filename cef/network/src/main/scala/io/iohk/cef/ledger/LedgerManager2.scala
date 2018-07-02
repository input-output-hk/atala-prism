package io.iohk.cef.ledger

import io.iohk.cef.ledger.identity2.Error

trait LedgerManager2 {

  type Transaction
  type LedgerState

  val LedgerId: Int

  def apply(currentState: LedgerState, transaction: Transaction): Either[Error, LedgerState]
}
