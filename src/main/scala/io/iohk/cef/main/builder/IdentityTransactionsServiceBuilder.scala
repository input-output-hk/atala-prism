package io.iohk.cef.main.builder

import akka.util.Timeout
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage

import scala.concurrent.ExecutionContext

trait FrontendServiceBuilder[S, H <: BlockHeader, T <: Transaction[S]] {}

trait IdentityTransactionServiceBuilder {
  self: NodeCoreBuilder[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction]
    with CommonTypeAliases[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction] =>

  def service(
      implicit
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: ByteStringSerializable[B],
      txByteStringSerializable: ByteStringSerializable[IdentityTransaction],
      stateyteStringSerializable: ByteStringSerializable[Set[SigningPublicKey]],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]): IdentityTransactionService =
    new IdentityTransactionService(nodeCore)
}
