package io.iohk.cef.ledger.storage

import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.{Block, Transaction}
import io.iohk.cef.codecs.nio._

trait LedgerStorage {

  def push[S, Tx <: Transaction[S]](ledgerId: LedgerId, block: Block[S, Tx])(
      implicit blockSerializable: NioEncDec[Block[S, Tx]]): Unit
}
