package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.language.higherKinds

trait LedgerStorage[F[_],
                    Key,
                    Value,
                    Header <: BlockHeader,
                    Tx <: Transaction[Key, Value]] {

  def push(block: Block[Key, Value, Header, Tx]): F[Unit]
}
