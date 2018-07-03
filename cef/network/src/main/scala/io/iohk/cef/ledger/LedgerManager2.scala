package io.iohk.cef.ledger

trait LedgerManager2[Error <: LedgerError] {

  trait Transaction extends (LedgerState => Either[Error, LedgerState])
  type LedgerState

  val LedgerId: String

  def apply(currentState: LedgerState, transaction: Transaction): Either[Error, LedgerState]
}
