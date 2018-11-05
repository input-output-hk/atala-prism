package io.iohk.cef.main.builder

import akka.util.Timeout
import io.iohk.cef.codecs.nio._
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.ledger.{BlockHeader, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage

import scala.concurrent.ExecutionContext

trait FrontendServiceBuilder[S, H <: BlockHeader, T <: Transaction[S]] {}

class IdentityTransactionServiceBuilder(
    nodeCoreBuilder: NodeCoreBuilder[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction],
    commonTypeAliases: CommonTypeAliases[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction]) {

  import commonTypeAliases._
  import nodeCoreBuilder._

  def service(
      implicit
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: NioEncDec[B],
      txByteStringSerializable: NioEncDec[IdentityTransaction],
      stateyteStringSerializable: NioEncDec[Set[SigningPublicKey]],
      dByteStringSerializable: NioEncDec[DiscoveryWireMessage]): IdentityTransactionService =
    new IdentityTransactionService(nodeCore)
}
