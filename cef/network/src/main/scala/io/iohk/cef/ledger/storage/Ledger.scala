package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger._
import io.iohk.cef.utils.ForExpressionsEnabler

import scala.language.higherKinds

case class Ledger[F[_],
                  State <: LedgerState[Key, _],
                  Key,
                  Header <: BlockHeader,
                  Tx <: Transaction[State, Key]](
                       ledgerStorage: LedgerStorage[F, State, Key, Header, Tx],
                       ledgerStateStorage: LedgerStateStorage[F, State, Key])(
                       implicit adapter: ForExpressionsEnabler[F]) {

  def apply(block: Block[State, Key, Header, Tx]): Either[LedgerError, F[Unit]] = {
    val state = ledgerStateStorage.slice(block.keys)
    for {
      updateResult <- block(state).map(newState => ledgerStateStorage.update(state, newState))
    } yield adapter.enable(updateResult).flatMap { _ => ledgerStorage.push(block) }
  }

  def slice(keys: Set[Key]): State =
    ledgerStateStorage.slice(keys)
}
