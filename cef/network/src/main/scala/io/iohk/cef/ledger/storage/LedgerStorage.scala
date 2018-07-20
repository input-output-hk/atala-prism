package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.{Block, BlockHeader, LedgerState, Transaction}
import io.iohk.cef.ledger.ByteStringSerializable

trait LedgerStorage {

  def push[State <: LedgerState[Key, _],
          Key,
          Header <: BlockHeader,
          Tx <: Transaction[State, Key]](ledgerId: Int, block: Block[State, Key, Header, Tx])(
                                        implicit blockSerializable: ByteStringSerializable[Block[State, Key, Header, Tx]]): Unit
}
