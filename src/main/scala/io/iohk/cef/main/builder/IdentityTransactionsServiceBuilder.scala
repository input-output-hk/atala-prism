package io.iohk.cef.main.builder

import akka.util.Timeout
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.codecs.array.ArrayCodecs._

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
      blockByteStringSerializable: ByteStringSerializable[B],
      txByteStringSerializable: ByteStringSerializable[IdentityTransaction],
      stateyteStringSerializable: ByteStringSerializable[Set[SigningPublicKey]],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      arrayEncoder: ArrayEncoder[B],
      arrayDecoder: ArrayDecoder[B],
      arrayLEncoder: ArrayEncoder[LogEntry[B]],
      arrayLDecoder: ArrayDecoder[LogEntry[B]]): IdentityTransactionService =
    new IdentityTransactionService(nodeCore)
}
