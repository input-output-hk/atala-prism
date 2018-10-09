package io.iohk.cef.main.builder
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.Network
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.network.encoding.nio.NioCodecs.{NioDecoder, NioEncoder}

trait NetworkBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: LedgerConfigBuilder with CommonTypeAliases[S, H, T] =>
  def txNetwork[ET: NioEncoder: NioDecoder](
      implicit dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]): Network[ET] =
    new Network[ET](networkDiscovery, transports)

  def blockNetwork[EB: NioEncoder: NioDecoder](
      implicit dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]): Network[EB] =
    new Network[EB](networkDiscovery, transports)
}
