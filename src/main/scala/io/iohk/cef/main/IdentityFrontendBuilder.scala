package io.iohk.cef.main
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.client.IdentityServiceApi
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.main.builder.base.CommonTypeAliases
import io.iohk.cef.main.builder.derived.ActorSystemBuilder
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.network.encoding.nio.NioCodecs._

import scala.concurrent.{ExecutionContext, Future}

trait IdentityFrontendBuilder {
  self: ActorSystemBuilder
    with IdentityTransactionServiceBuilder
    with CommonTypeAliases[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction] =>

  implicit val system = actorSystem
  implicit val materializer = ActorMaterializer()

  def bindingFuture(
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
                     lByteStringSerializable: ByteStringSerializable[LogEntry[B]]): Future[Http.ServerBinding] =
    {
      val serviceApi = new IdentityServiceApi(service)
      val route = serviceApi.createIdentity
      Http()(actorSystem).bindAndHandle(route, "localhost", 8888)
    }
}
