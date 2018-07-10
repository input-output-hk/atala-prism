package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.{Block, LedgerState}

import scala.language.higherKinds

trait LedgerStorage[F[_], State <: LedgerState[Key, _], Key] {

  def push(block: Block[State, Key]): F[Unit]
}
