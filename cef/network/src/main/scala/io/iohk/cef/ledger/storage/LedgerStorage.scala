package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.{Block, BlockHeader, LedgerState, Transaction}
import io.iohk.cef.ledger.ByteStringSerializable

import scala.language.higherKinds

trait LedgerStorage[F[_]] {

  def push[State <: LedgerState[Key, _],
          Key,
          Header <: BlockHeader,
          Tx <: Transaction[State, Key]](ledgerId: Int, block: Block[State, Key, Header, Tx])(
                                        implicit blockSerializable: ByteStringSerializable[Block[State, Key, Header, Tx]]): F[Unit]
}
