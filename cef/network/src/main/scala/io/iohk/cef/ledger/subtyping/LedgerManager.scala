package io.iohk.cef.ledger.subtyping

import io.iohk.cef.ledger.LedgerError

trait LedgerManager {

  type LedgerState
  trait Transaction extends (LedgerState => Either[LedgerError, LedgerState])

  class Block(height: Int, transactions: List[Transaction]) {
    def stateTransition: LedgerState => Either[LedgerError, LedgerState] = ledgerState => {
      transactions.foldLeft[Either[LedgerError, LedgerState]](Right(ledgerState))((either, tx) => {
        either.flatMap(tx(_))
      })
    }
  }

  val LedgerId: String

  def apply(currentState: LedgerState, block: Block): Either[LedgerError, LedgerState]
}
