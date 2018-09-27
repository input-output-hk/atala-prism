package io.iohk.cef.main.builder.base
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.network.Network

trait NetworkBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  val txNetwork: Network[T]
  val blockNetwork: Network[Block[S, H, T]]
}
