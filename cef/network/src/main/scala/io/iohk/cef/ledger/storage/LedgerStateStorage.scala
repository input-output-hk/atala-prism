package io.iohk.cef.ledger.storage

import akka.util.ByteString
import io.iohk.cef.ledger.LedgerState

import scala.language.higherKinds

trait LedgerStateStorage[F[_], State <: LedgerState[Key, _], Key] {

  def slice(keys: Set[Key]): State

  def update(previousHash: ByteString, newState: State): F[Unit]
}
