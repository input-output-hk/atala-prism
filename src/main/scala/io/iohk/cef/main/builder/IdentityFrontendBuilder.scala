package io.iohk.cef.main.builder
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.iohk.cef.consensus.raft.LogEntry
import io.iohk.cef.frontend.client.IdentityServiceApi
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.crypto._
import io.iohk.cef.network.encoding.array.ArrayCodecs.{ArrayDecoder, ArrayEncoder}

import scala.concurrent.{ExecutionContext, Future}

class IdentityFrontendBuilder(
    actorSystemBuilder: DefaultActorSystemBuilder,
    identityTransactionServiceBuilder: IdentityTransactionServiceBuilder,
    commonTypeAliases: CommonTypeAliases[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction],
    configReaderBuilder: ConfigReaderBuilder) {

  import actorSystemBuilder._
  import identityTransactionServiceBuilder._
  import commonTypeAliases._
  import configReaderBuilder._

  implicit val system = actorSystem
  implicit val materializer = ActorMaterializer()

  def bindingFuture(
      implicit
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: ByteStringSerializable[B],
      stateyteStringSerializable: ByteStringSerializable[Set[SigningPublicKey]],
      ebByteStringSerializable: ByteStringSerializable[IdentityTransaction],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      arrayEncoder: ArrayEncoder[B],
      arrayDecoder: ArrayDecoder[B],
      arrayLEncoder: ArrayEncoder[LogEntry[B]],
      arrayLDecoder: ArrayDecoder[LogEntry[B]]): Future[Http.ServerBinding] = {
    val serviceApi = new IdentityServiceApi(service)
    val route = serviceApi.createIdentity
    Http()(actorSystem)
      .bindAndHandle(route, config.getString("frontend.rest.interface"), config.getInt("frontend.rest.port"))
  }
}
