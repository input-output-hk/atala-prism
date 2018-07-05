package io.iohk.cef.ledger.persistence

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Ledger[State <: LedgerState](ledgerStorage: LedgerStorage[Future, State], ledgerState: State) {
  def apply(block: Block[State]): Future[Ledger[State]] = {
    for {
      ledger <- block.apply(ledgerState).map(state => Ledger(ledgerStorage, state))
      _ <- ledgerStorage.push(block)
    } yield ledger
  }
}
