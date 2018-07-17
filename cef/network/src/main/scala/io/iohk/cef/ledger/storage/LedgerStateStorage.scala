package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.LedgerState

import scala.language.higherKinds

trait LedgerStateStorage[F[_], Key, Value] {

  def slice(keys: Set[Key]): LedgerState[Key, Value]

  def update(previousState: LedgerState[Key, Value], newState: LedgerState[Key, Value]): F[Unit]
}
