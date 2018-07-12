package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.{Block, BlockHeader, LedgerState, Transaction}

import scala.language.higherKinds

trait LedgerStorage[F[_],
                    State <: LedgerState[Key, _],
                    Key,
                    Header <: BlockHeader,
                    Tx <: Transaction[State, Key]] {

  def push(block: Block[State, Key, Header, Tx]): F[Unit]
}
