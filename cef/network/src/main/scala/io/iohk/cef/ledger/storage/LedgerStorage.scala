package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}

trait LedgerStorage {

  def push[Key,
          Value,
          Header <: BlockHeader,
          Tx <: Transaction[Key, Value]](ledgerId: Int, block: Block[Key, Value, Header, Tx])(
                                        implicit blockSerializable: ByteStringSerializable[Block[Key, Value, Header, Tx]]): Unit
}
