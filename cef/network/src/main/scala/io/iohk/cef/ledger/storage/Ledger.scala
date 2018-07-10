package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger._
import io.iohk.cef.utils.ForExpressionsEnabler

import scala.language.higherKinds

case class Ledger[State <: LedgerState[Key, _], Key, F[_]](
                                                     ledgerStorage: LedgerStorage[F, State, Key],
                                                     ledgerStateStorage: LedgerStateStorage[F, State, Key])(
                                                     implicit adapter: ForExpressionsEnabler[F]) {

  def apply(block: Block[State, Key]): Either[LedgerError, F[Unit]] = {
    //TODO: Eliminage .get
    val state = ledgerStateStorage.slice(block.keys).get
    for {
      updateResult <- block(state).map(newState => ledgerStateStorage.update(state.hash, newState))
    } yield adapter.enable(updateResult).flatMap { u => ledgerStorage.push(block) }
  }

  def slice(keys: Set[Key]): State =
    //TODO: Eliminate .get
    ledgerStateStorage.slice(keys).get
}
