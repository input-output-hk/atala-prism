package io.iohk.cef.ledger.identity2

trait LedgerManager2 {

  type Transaction
  type LedgerState

  val LedgerId: Int

  def apply(currentState: LedgerState, transaction: Transaction): Either[Error, LedgerState]
}
