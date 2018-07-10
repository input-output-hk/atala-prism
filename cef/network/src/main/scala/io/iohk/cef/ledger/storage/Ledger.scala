package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Ledger[State <: LedgerState[Key, _], Key](
                                                     ledgerStorage: LedgerStorage[Future, State, Key],
                                                     ledgerStateStorage: LedgerStateStorage[Future, State, Key]) {
  def apply(block: Block[State, Key]): Either[LedgerError, Future[Unit]] = {
    val state = ledgerStateStorage.slice(block.keys)
    for {
      updateResult <- block(state).map(newState => ledgerStateStorage.update(state.hash, newState))
    } yield updateResult.flatMap { u => ledgerStorage.push(block) }
  }

  def slice(keys: Set[Key]): State = ledgerStateStorage.slice(keys)
}
