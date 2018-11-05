package io.iohk.cef.main.builder
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.iohk.cef.codecs.nio._
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.controllers.IdentitiesController
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage

import scala.concurrent.{ExecutionContext, Future}

class IdentityFrontendBuilder(
    actorSystemBuilder: DefaultActorSystemBuilder,
    identityTransactionServiceBuilder: IdentityTransactionServiceBuilder,
    commonTypeAliases: CommonTypeAliases[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction],
    configReaderBuilder: ConfigReaderBuilder) {

  import actorSystemBuilder._
  import commonTypeAliases._
  import configReaderBuilder._
  import identityTransactionServiceBuilder._

  implicit val system = actorSystem
  implicit val materializer = ActorMaterializer()

  def bindingFuture(
      implicit
      timeout: Timeout,
      executionContext: ExecutionContext,
      blockByteStringSerializable: NioEncDec[B],
      stateyteStringSerializable: NioEncDec[Set[SigningPublicKey]],
      ebByteStringSerializable: NioEncDec[IdentityTransaction],
      dByteStringSerializable: NioEncDec[DiscoveryWireMessage]): Future[Http.ServerBinding] = {
    val serviceApi = new IdentitiesController(service)
    val route = serviceApi.routes
    Http()(actorSystem)
      .bindAndHandle(route, config.getString("frontend.rest.interface"), config.getInt("frontend.rest.port"))
  }
}
