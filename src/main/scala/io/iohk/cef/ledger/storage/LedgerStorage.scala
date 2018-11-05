package io.iohk.cef.ledger.storage

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.codecs.nio._

trait LedgerStorage {

  def push[S, Header <: BlockHeader, Tx <: Transaction[S]](ledgerId: LedgerId, block: Block[S, Header, Tx])(
      implicit blockSerializable: NioEncDec[Block[S, Header, Tx]]): Unit
}
