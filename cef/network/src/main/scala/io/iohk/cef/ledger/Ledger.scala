package io.iohk.cef.ledger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Ledger[State <: LedgerState](ledgerStorage: LedgerStorage[Future, State], ledgerState: State) {
  def apply(block: Block[State]): Future[Either[LedgerError, Ledger[State]]] = {
    for {
      _<- ledgerStorage.push(block)
    } yield block.apply(ledgerState).map(state => Ledger(ledgerStorage, state))
  }
}
