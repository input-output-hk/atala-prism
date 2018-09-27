package io.iohk.cef.main
import io.iohk.cef.core.NodeCore
import io.iohk.cef.ledger.{BlockHeader, Transaction}

trait NodeCoreBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  val nodeCore: NodeCore[S, H, T]
}
