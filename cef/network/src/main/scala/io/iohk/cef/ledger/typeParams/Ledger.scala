package io.iohk.cef.ledger.typeParams

import io.iohk.cef.ledger.LedgerError

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Ledger[State <: LedgerState](ledgerStorage: LedgerStorage[Future, State], ledgerState: State) {
  def apply(block: Block[State]): Future[Either[LedgerError, Ledger[State]]] = {
    for {
      _<- ledgerStorage.push(block)
    } yield block.stateTransition(ledgerState).map(state => Ledger(ledgerStorage, state))
  }
}
