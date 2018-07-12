package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger._
import io.iohk.cef.utils.ForExpressionsEnabler

import scala.language.higherKinds

case class Ledger[State <: LedgerState[Key, _], Key, F[_], Header <: BlockHeader, Tx <: Transaction[State, Key]](
                                                     ledgerStorage: LedgerStorage[F, State, Key, Header, Tx],
                                                     ledgerStateStorage: LedgerStateStorage[F, State, Key, Header, Tx])(
                                                     implicit adapter: ForExpressionsEnabler[F]) {

  def apply(block: Block[State, Key, Header, Tx]): Either[LedgerError, F[Unit]] = {
    val state = ledgerStateStorage.slice(block.keys)
    for {
      updateResult <- block(state).map(newState => ledgerStateStorage.update(state.hash, newState))
    } yield adapter.enable(updateResult).flatMap { _ => ledgerStorage.push(block) }
  }

  def slice(keys: Set[Key]): State =
    ledgerStateStorage.slice(keys)
}
