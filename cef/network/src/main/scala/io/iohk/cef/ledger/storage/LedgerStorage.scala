package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}

trait LedgerStorage {

  def push[S,
          Header <: BlockHeader,
          Tx <: Transaction[S]](ledgerId: Int, block: Block[S, Header, Tx])(
                                        implicit blockSerializable: ByteStringSerializable[Block[S, Header, Tx]]): Unit
}
