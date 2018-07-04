package io.iohk.cef.ledger.typeParams

import io.iohk.cef.ledger.LedgerError

case class Block[State <: LedgerState](height: Int, transactions: List[Transaction[State]]) {
  def stateTransition: State => Either[LedgerError, State] = ledgerState => {
    transactions.foldLeft[Either[LedgerError, State]](Right(ledgerState))((either, tx) => {
      either.flatMap(tx(_))
    })
  }
}
