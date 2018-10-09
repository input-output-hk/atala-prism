package io.iohk.cef.main.builder
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

trait CommonTypeAliases[S, H <: BlockHeader, T <: Transaction[S]] {
  type B = Block[S, H, T]
}
