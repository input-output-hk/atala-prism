package io.iohk.cef.main.builder.derived

import io.iohk.cef.core.Envelope
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.main.builder.base.LedgerConfigBuilder
import io.iohk.cef.network.Network
import io.iohk.cef.network.encoding.nio.NioCodecs._

trait NetworkBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: LedgerConfigBuilder =>
  type EB = Envelope[Block[S, H, T]]
  type ET = Envelope[T]
  def txNetwork[ET: NioEncoder: NioDecoder]: Network[ET] =
    new Network[ET](networkDiscovery, transports)

  def blockNetwork[EB: NioEncoder: NioDecoder]: Network[EB] =
    new Network[EB](networkDiscovery, transports)
}
