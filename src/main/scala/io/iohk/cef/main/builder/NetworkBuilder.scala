package io.iohk.cef.main.builder
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.Network
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.codecs.nio.NioCodecs.{NioDecoder, NioEncoder}

class NetworkBuilder[S, H <: BlockHeader, T <: Transaction[S]](ledgerConfigBuilder: LedgerConfigBuilder) {
  import ledgerConfigBuilder._

  def txNetwork[ET: NioEncoder: NioDecoder](
      implicit dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]): Network[ET] =
    new Network[ET](networkDiscovery, transports)

  def blockNetwork[EB: NioEncoder: NioDecoder](
      implicit dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]): Network[EB] =
    new Network[EB](networkDiscovery, transports)
}
