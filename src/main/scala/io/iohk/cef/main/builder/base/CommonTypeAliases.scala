package io.iohk.cef.main.builder.base
import io.iohk.cef.core.Envelope
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

trait CommonTypeAliases[S, H <: BlockHeader, T <: Transaction[S]] {
  type B = Block[S, H, T]
  type EB = Envelope[B]
  type ET = Envelope[T]
}
