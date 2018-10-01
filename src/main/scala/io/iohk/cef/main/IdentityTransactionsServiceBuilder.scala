package io.iohk.cef.main
import akka.util.Timeout
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.main.builder.base.CommonTypeAliases
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.network.encoding.nio.NioCodecs.{NioDecoder, NioEncoder}

import scala.concurrent.ExecutionContext

trait FrontendServiceBuilder[S, H <: BlockHeader, T <: Transaction[S]] {}

trait IdentityTransactionServiceBuilder {
  self: NodeCoreBuilder[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction]
  with CommonTypeAliases[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction] =>

  def service(
               implicit
               txNetworkEncoder: NioEncoder[ET],
               txNetworkDecoder: NioDecoder[ET],
               blockNetworkEncoder: NioEncoder[EB],
               blockNetworkDecoder: NioDecoder[EB],
               timeout: Timeout,
               executionContext: ExecutionContext,
               blockByteStringSerializable: ByteStringSerializable[B],
               stateyteStringSerializable: ByteStringSerializable[Set[SigningPublicKey]],
               ebByteStringSerializable: ByteStringSerializable[EB],
               etStringSerializable: ByteStringSerializable[ET],
               dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
               lByteStringSerializable: ByteStringSerializable[LogEntry[B]]) =
    new IdentityTransactionService(nodeCore)
}
